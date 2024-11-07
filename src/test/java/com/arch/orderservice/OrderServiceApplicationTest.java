package com.arch.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.arch.orderservice.book.Book;
import com.arch.orderservice.book.BookClient;
import com.arch.orderservice.domain.Order;
import com.arch.orderservice.domain.OrderStatus;
import com.arch.orderservice.event.OrderAcceptedMessage;
import com.arch.orderservice.web.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class OrderServiceApplicationTest {

  @Container
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.2-alpine3.19");

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private OutputDestination outputDestination;

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private BookClient bookClient;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", OrderServiceApplicationTest::r2dbcUrl);
    registry.add("spring.r2dbc.username", postgreSQLContainer::getUsername);
    registry.add("spring.r2dbc.password", postgreSQLContainer::getPassword);
    registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl);
  }

  private static String r2dbcUrl() {
    return String.format("r2dbc:postgresql://%s:%s/%s",
        postgreSQLContainer.getHost(),
        postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
        postgreSQLContainer.getDatabaseName());
  }

  @Test
  void whenGetOrdersThenReturn() throws IOException {
    String bookIsbn = "1234567893";
    Book book = new Book(bookIsbn, "Title", "Author", BigDecimal.valueOf(9.90));
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 1);
    Order expectedOrder = webTestClient.post().uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class).returnResult().getResponseBody();
    assertThat(expectedOrder).isNotNull();
    assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
        .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

    webTestClient.get().uri("/orders")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(Order.class).value(orders -> {
          assertThat(orders.stream().filter(order -> order.bookIsbn().equals(bookIsbn)).findAny()).isNotEmpty();
        });
  }

  @Test
  void whenPostRequestAndBookExistsThenOrderAccepted() throws IOException {
    String bookIsbn = "1234567899";
    Book book = new Book(bookIsbn, "Title", "Author", BigDecimal.valueOf(9.90));
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

    Order createdOrder = webTestClient.post().uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class).returnResult().getResponseBody();

    assertThat(createdOrder).isNotNull();
    assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
    assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
    assertThat(createdOrder.bookName()).isEqualTo(book.title() + "-" + book.author());
    assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
    assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);

    assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
        .isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
  }

  @Test
  void whenPostRequestAndBookNotExistsThenOrderRejected() {
    String bookIsbn = "1234567894";
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty());
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

    Order createdOrder = webTestClient.post().uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class).returnResult().getResponseBody();

    assertThat(createdOrder).isNotNull();
    assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
    assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
    assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
  }
}

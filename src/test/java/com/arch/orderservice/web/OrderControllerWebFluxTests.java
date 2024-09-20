package com.arch.orderservice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.arch.orderservice.domain.Order;
import com.arch.orderservice.domain.OrderService;
import com.arch.orderservice.domain.OrderStatus;

import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
class OrderControllerWebFluxTests {

  @Autowired
  private WebTestClient webClient;

  @MockBean
  private OrderService orderService;

  @Test
  void whenBookNotAvailableThenRejectOrder() {
    OrderRequest request = new OrderRequest("1234567890", 3);
    Order expectedOrder = OrderService.buildRejectedOrder(request.isbn(), request.quantity());

    given(orderService.submitOrder(request.isbn(), request.quantity()))
        .willReturn(Mono.just(expectedOrder));

    webClient.post()
        .uri("/orders")
        .bodyValue(request)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class).value(actualOrder -> {
          assertThat(actualOrder).isNotNull();
          assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
        });

  }
}

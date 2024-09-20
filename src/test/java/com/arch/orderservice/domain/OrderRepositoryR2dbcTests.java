package com.arch.orderservice.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.arch.orderservice.config.DataConfig;

import reactor.test.StepVerifier;

@DataR2dbcTest
@Import(DataConfig.class)
@Testcontainers
class OrderRepositoryR2dbcTests {

  @Container
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.2-alpine3.19");

  @Autowired
  OrderRepository orderRepository;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", OrderRepositoryR2dbcTests::r2dbcUrl);
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
  void createRejectedOrder() {
    Order rejectedOrder = OrderService.buildRejectedOrder("1234567890",3);

    StepVerifier.create(orderRepository.save(rejectedOrder))
        .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
        .verifyComplete();
  }
}

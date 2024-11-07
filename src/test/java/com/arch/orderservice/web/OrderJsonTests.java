package com.arch.orderservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import com.arch.orderservice.domain.Order;
import com.arch.orderservice.domain.OrderStatus;

@JsonTest
class OrderJsonTests {

  @Autowired
  private JacksonTester<Order> json;

  @Test
  void testSerialize() throws IOException {
    var order = new Order(394L, "1234567890", "Book name", new BigDecimal(100), 1, OrderStatus.ACCEPTED, Instant.now(),
        Instant.now(), 21);
    var jsonContent = json.write(order);
    assertThat(jsonContent).extractingJsonPathNumberValue("@.id").isEqualTo(order.id().intValue());
    assertThat(jsonContent).extractingJsonPathStringValue("@.bookIsbn").isEqualTo(order.bookIsbn());
    assertThat(jsonContent).extractingJsonPathStringValue("@.bookName").isEqualTo(order.bookName());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.bookPrice").isEqualTo(order.bookPrice().intValue());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.quantity").isEqualTo(order.quantity());
    assertThat(jsonContent).extractingJsonPathStringValue("@.status").isEqualTo(order.status().toString());
    assertThat(jsonContent).extractingJsonPathStringValue("@.createdDate").isEqualTo(order.createdDate().toString());
    assertThat(jsonContent).extractingJsonPathStringValue("@.lastModifiedDate").isEqualTo(order.lastModifiedDate().toString());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.version").isEqualTo(order.version());
  }
}

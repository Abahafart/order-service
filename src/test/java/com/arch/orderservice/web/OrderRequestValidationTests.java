package com.arch.orderservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class OrderRequestValidationTests {

  private static Validator validator;
  private static final String ISBN = "1234567890";
  private static final int QUANTITY = 1;

  @BeforeAll
  static void beforeAll() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void whenAllFieldsCorrectThenValidationSucceeds() {
    OrderRequest orderRequest = new OrderRequest(ISBN, QUANTITY);
    Set<ConstraintViolation<OrderRequest>> constraintViolations = validator.validate(orderRequest);
    assertThat(constraintViolations).isEmpty();
  }

  @Test
  void whenIsbnNotDefinedThenValidationFails() {
    var orderRequest = new OrderRequest("", QUANTITY);
    Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("The book ISBN must be defined.");
  }

  @Test
  void whenQuantityIsNotDefinedThenValidationFails() {
    var orderRequest = new OrderRequest(ISBN, null);
    Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("The book quantity must be defined.");
  }

  @Test
  void whenQuantityIsLowerThanMinThenValidationFails() {
    var orderRequest = new OrderRequest(ISBN, 0);
    Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("You must order at least 1 item.");
  }

  @Test
  void whenQuantityIsGreaterThanMaxThenValidationFails() {
    var orderRequest = new OrderRequest(ISBN, 7);
    Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("You cannot order more than 5 items.");
  }
}

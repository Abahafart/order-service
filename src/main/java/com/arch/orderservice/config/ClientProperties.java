package com.arch.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "catalog")
public record ClientProperties(@NotNull String uri) {
}

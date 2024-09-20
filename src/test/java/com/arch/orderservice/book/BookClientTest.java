package com.arch.orderservice.book;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BookClientTest {

  private MockWebServer mockWebServer;
  private BookClient bookClient;

  @BeforeEach
  void setUp() throws IOException {
    this.mockWebServer = new MockWebServer();
    this.mockWebServer.start();
    WebClient webClient = WebClient.builder().baseUrl(this.mockWebServer.url("/").toString()).build();
    this.bookClient = new BookClient(webClient);
  }

  @AfterEach
  void clean() throws IOException {
    this.mockWebServer.shutdown();
  }

  @Test
  void whenBookExistsThenReturnBook() {
    String isbn = "1234567890";
    MockResponse mockResponse = new MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody("""
          {
          "isbn": %s,
          "title": "Title",
          "author": "Author",
          "price": 9.90,
          "publisher": "Polarsophia"
          }
        """.formatted(isbn));

    mockWebServer.enqueue(mockResponse);

    Mono<Book> book = bookClient.getBookByIsbn(isbn);

    StepVerifier.create(book)
        .expectNextMatches(b -> b.isbn().equals(isbn))
        .verifyComplete();
  }
}
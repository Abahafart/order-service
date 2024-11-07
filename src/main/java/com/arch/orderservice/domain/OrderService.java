package com.arch.orderservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arch.orderservice.book.Book;
import com.arch.orderservice.book.BookClient;
import com.arch.orderservice.event.OrderAcceptedMessage;
import com.arch.orderservice.event.OrderDispatchedMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final BookClient bookClient;
  private final StreamBridge streamBridge;

  public OrderService(OrderRepository orderRepository, BookClient bookClient, StreamBridge streamBridge) {
    this.orderRepository = orderRepository;
    this.bookClient = bookClient;
    this.streamBridge = streamBridge;
  }

  public Flux<Order> getAllOrders() {
    return orderRepository.findAll();
  }

  @Transactional//Executes the method in a local transaction
  public Mono<Order> submitOrder(String isbn, int quantity) {
    return bookClient.getBookByIsbn(isbn)
        .map(book -> buildAcceptedOrder(book, quantity))
        .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
        .flatMap(orderRepository::save)//save the order in database
        .doOnNext(this::publishOrderAcceptedEvent);//Publishes an event if the order is accepted
  }

  public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
    return flux.flatMap(message -> orderRepository.findById(message.orderId()))//for each object emitted to the stream, it reads the related order from database
        .map(this::buildDispatchedOrder)//updates the order with the dispatched status
        .flatMap(orderRepository::save);//saves the updated order in the database
  }

  private Order buildDispatchedOrder(Order existingOrder) {
    return new Order(//given an order, it returns a new record with the dispatched status
        existingOrder.id(), existingOrder.bookIsbn(), existingOrder.bookName(), existingOrder.bookPrice(), existingOrder.quantity(),
        OrderStatus.DISPATCHED, existingOrder.createdDate(), existingOrder.lastModifiedDate(), existingOrder.version()
    );
  }

  private void publishOrderAcceptedEvent(Order order) {
    if (!order.status().equals(OrderStatus.ACCEPTED)) {
      return;
    }
    var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
    log.info("Sending order accepted event with id {}", order.id());
    var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);//Explicity sends a message to acceptOrder-out-0 the binding
    log.info("Result of sending data for order with id {}: {}", order.id(), result);
  }

  public static Order buildAcceptedOrder(Book book, int quantity) {
    return Order.of(book.isbn(), book.title() + "-" + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
  }

  public static Order buildRejectedOrder(String isbn, int quantity) {
    return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
  }
}

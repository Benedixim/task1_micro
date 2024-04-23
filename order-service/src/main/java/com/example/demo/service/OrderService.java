package com.example.demo.service;

import com.example.demo.dto.DeleteResponse;
import com.example.demo.dto.InventoryResponse;
import com.example.demo.dto.OrderLineItemsDto;
import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.model.OrderLineItems;
import com.example.demo.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.example.demo.dto.OrderRequest;
import com.example.demo.model.Order;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        List<Integer> quantities = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getQuantity)
                .toList();

        InventoryResponse[] inventoryResponsArray = webClientBuilder.build().get()
                .uri("http://inventory-service/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductIsInStock = Arrays.stream(inventoryResponsArray)
                .allMatch(InventoryResponse::isInStock); // check if all elements in inventoryResponsArray)

        if(allProductIsInStock){
            orderRepository.save(order);

            webClientBuilder.build().delete()
                    .uri("http://inventory-service/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes)
                                                    .queryParam("quantity", quantities)
                                                    .build())
                    .retrieve()
                    .bodyToMono(DeleteResponse[].class)
                    .block();

            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent( order.getOrderNumber()));


        } else {
            throw new IllegalArgumentException("Product is not in stock");
        }

    }



    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }

}

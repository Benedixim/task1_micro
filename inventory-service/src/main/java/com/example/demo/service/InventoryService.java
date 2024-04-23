package com.example.demo.service;

import com.example.demo.dto.DeleteResponse;
import com.example.demo.dto.InventoryResponse;
import com.example.demo.model.Inventory;
import com.example.demo.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCode) {
        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(inventory ->
                    InventoryResponse.builder()
                            .isInStock(inventory.getQuantity() > 0)
                            .skuCode(inventory.getSkuCode())
                            .build()
                ).toList();
    }

    @Transactional
    public List<DeleteResponse> deleteItems(List<String> skuCodes, List<Integer> quantities) {
        List<DeleteResponse> responses = new ArrayList<>();

        for (int i = 0; i < skuCodes.size(); i++) {
            String code = skuCodes.get(i);
            int qty = quantities.get(i);

            Inventory inventory = inventoryRepository.findBySkuCode(code).stream().toList().get(0);
            if (inventory != null && inventory.getQuantity() >= qty) {
                inventory.setQuantity(inventory.getQuantity() - qty);
                inventoryRepository.save(inventory);

                responses.add(new DeleteResponse(code, qty));
            } else {
                throw new IllegalArgumentException("Not enough items in stock for SKU code: " + code);
            }
        }

        return responses;
    }
}

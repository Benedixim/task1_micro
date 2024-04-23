package com.example.demo.controller;

import com.example.demo.dto.DeleteResponse;
import com.example.demo.dto.InventoryResponse;
import com.example.demo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(List<String> skuCode) {
        return inventoryService.isInStock(skuCode);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public List<DeleteResponse> deleteItems(@RequestParam List<String> skuCode, @RequestParam List<Integer> quantity) {
        return inventoryService.deleteItems(skuCode, quantity);
    }
}

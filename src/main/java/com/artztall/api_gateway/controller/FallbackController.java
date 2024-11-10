package com.artztall.api_gateway.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public ResponseEntity<String> userServiceFallback() {
        return ResponseEntity.ok("User Service is taking longer than expected. Please try again later.");
    }

    @GetMapping("/product")
    public ResponseEntity<String> productServiceFallback() {
        return ResponseEntity.ok("Product Service is taking longer than expected. Please try again later.");
    }

    @GetMapping("/order")
    public ResponseEntity<String> orderServiceFallback() {
        return ResponseEntity.ok("Order Service is taking longer than expected. Please try again later.");
    }

    @GetMapping("/payment")
    public ResponseEntity<String> paymentServiceFallback() {
        return ResponseEntity.ok("Payment Service is taking longer than expected. Please try again later.");
    }

    @GetMapping("/auction")
    public ResponseEntity<String> auctionServiceFallback() {
        return ResponseEntity.ok("Auction Service is taking longer than expected. Please try again later.");
    }
}
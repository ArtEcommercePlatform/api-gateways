package com.artztall.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class GatewayConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private final JwtAuthFilter jwtAuthFilter;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Collections.singletonList(allowedOrigins));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-public", r -> r
                        .path("/api/v1/auth/register", "/api/v1/auth/authenticate")
                        .filters(f -> f
                                .rewritePath("/api/v1/auth/(?<segment>.*)", "/${segment}"))
                        .uri("lb://user-service"))
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("userService")
                                        .setFallbackUri("forward:/fallback/user"))
                                .rewritePath("/api/users/(?<segment>.*)", "/${segment}"))
                        .uri("lb://user-service"))
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("productService")
                                        .setFallbackUri("forward:/fallback/product"))
                                .rewritePath("/api/products/(?<segment>.*)", "/${segment}"))
                        .uri("lb://product-service"))
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("orderService")
                                        .setFallbackUri("forward:/fallback/order"))
                                .rewritePath("/api/orders/(?<segment>.*)", "/${segment}"))
                        .uri("lb://order-service"))
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("paymentService")
                                        .setFallbackUri("forward:/fallback/payment"))
                                .rewritePath("/api/payments/(?<segment>.*)", "/${segment}"))
                        .uri("lb://payment-service"))
                .route("auction-service", r -> r
                        .path("/api/auctions/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("auctionService")
                                        .setFallbackUri("forward:/fallback/auction"))
                                .rewritePath("/api/auctions/(?<segment>.*)", "/${segment}"))
                        .uri("lb://auction-service"))
                .build();
    }
}
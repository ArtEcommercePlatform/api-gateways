package com.artztall.api_gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey key;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = java.util.Arrays.copyOf(keyBytes, 32);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for public endpoints
            if (!isSecuredPath(request.getPath().value())) {
                return chain.filter(exchange);
            }

            try {
                // Extract and validate token
                String token = extractToken(request);
                if (token == null) {
                    return handleError(exchange.getResponse(), "Authorization header is missing or invalid", HttpStatus.UNAUTHORIZED);
                }

                // Validate token
                Claims claims = validateToken(token);
                if (claims == null) {
                    return handleError(exchange.getResponse(), "Invalid token", HttpStatus.UNAUTHORIZED);
                }

                // Simply continue with the original request if token is valid
                return chain.filter(exchange);

            } catch (JwtException e) {
                logger.error("JWT validation failed: {}", e.getMessage());
                return handleError(exchange.getResponse(), "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                logger.error("Unexpected error during authentication", e);
                return handleError(exchange.getResponse(), "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private boolean isSecuredPath(String path) {
        List<String> publicPaths = List.of(
                "/api/auth/login",
                "/api/auth/signup",
                "/api/auth/verify",
                "/api/public",
                "/api/products"
        );

        return publicPaths.stream()
                .noneMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String headerValue = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (headerValue != null && headerValue.startsWith("Bearer ")) {
            return headerValue.substring(7);
        }
        return null;
    }

    private Claims validateToken(String token) throws JwtException {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.error("JWT validation error: {}", e.getMessage());
            throw e;
        }
    }

    private Mono<Void> handleError(ServerHttpResponse response, String message, HttpStatus status) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorJson = String.format("{\"error\": \"%s\", \"status\": %d}",
                message.replace("\"", "\\\""),
                status.value());

        DataBuffer buffer = response.bufferFactory()
                .wrap(errorJson.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties if needed
    }
}
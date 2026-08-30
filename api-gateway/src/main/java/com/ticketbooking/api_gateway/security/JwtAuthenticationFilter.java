package com.ticketbooking.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter implements WebFilter {
    private static final List<String> PUBLIC_PREFIXES = List.of("/auth/", "/event");

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicRequest(exchange)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key()).build()
                    .parseClaimsJws(authorization.substring(7)).getBody();
            if (!"access".equals(claims.get("type", String.class))) {
                return unauthorized(exchange);
            }
            String userUuid = claims.get("userUuid", String.class);
            if (userUuid == null || userUuid.isBlank()) {
                return unauthorized(exchange);
            }

            ServerHttpRequest trustedRequest = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                        headers.set("X-User-Id", userUuid);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(trustedRequest).build());
        } catch (Exception exception) {
            return unauthorized(exchange);
        }
    }

    private boolean isPublicRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/auth/") || (path.startsWith("/event") && exchange.getRequest().getMethod().name().equals("GET"))) return true;
        if (path.equals("/payment/webhook")) return true;
        if (path.startsWith("/booking/event/") && path.endsWith("/seats") && exchange.getRequest().getMethod().name().equals("GET")) return true;
        return path.equals("/user") && exchange.getRequest().getMethod().name().equals("POST");
    }

    private SecretKey key() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)); }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}

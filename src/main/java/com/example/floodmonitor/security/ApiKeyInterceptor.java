package com.example.floodmonitor.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

// prüft vor jeder API-Anfrage den API-Key im Header X-API-KEY
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("${api.key}")
    private String apiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = request.getHeader("X-API-KEY");

        // kein Key -> 401 Unauthorized
        if (key == null || key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "API-Key fehlt. Bitte den Header X-API-KEY mitschicken");
        }

        // falscher Key -> 403 Forbidden
        if (!key.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API-Key ist ungültig");
        }

        return true;
    }
}
package com.rob.products.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyProperties apiKeyProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = null;
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            apiKey = bearer.substring(7);
        }
        if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Resolving key='{}' — known keys: {}", apiKey, apiKeyProperties.getApiKeys().keySet());
            String rolesValue = apiKeyProperties.getApiKeys().get(apiKey);
            if (rolesValue != null) {
                List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesValue.split(","))
                        .map(String::trim)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(apiKey, null, authorities));
            }
        }
        filterChain.doFilter(request, response);
    }
}
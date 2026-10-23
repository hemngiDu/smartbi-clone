package com.smartbi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final ApiSecurityProperties properties;
    public ApiKeyAuthenticationFilter(ApiSecurityProperties properties) { this.properties = properties; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) { chain.doFilter(request, response); return; }
        String supplied = request.getHeader("X-API-Key");
        var identity = properties.getApiKeys().entrySet().stream()
            .filter(e -> supplied != null && e.getValue() != null && !e.getValue().isBlank() && MessageDigest.isEqual(e.getValue().getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
            .map(java.util.Map.Entry::getKey).findFirst();
        if (identity.isEmpty()) {
            response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"A valid X-API-Key is required\"}"); return;
        }
        String role = properties.getRoles().getOrDefault(identity.get(), "VIEWER").toUpperCase(java.util.Locale.ROOT);
        var authentication = new UsernamePasswordAuthenticationToken(identity.get(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try { chain.doFilter(request, response); } finally { SecurityContextHolder.clearContext(); }
    }
}

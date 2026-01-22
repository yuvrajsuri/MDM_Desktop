package com.company.mdm.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiTokenFilter extends OncePerRequestFilter {

    private final String expectedApiKey;
    private final String apiKeyHeader;

    public ApiTokenFilter(String expectedApiKey, String apiKeyHeader) {
        this.expectedApiKey = expectedApiKey;
        this.apiKeyHeader = apiKeyHeader;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // allow actuator endpoints without API key
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(apiKeyHeader);

        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                        {
                          "error": "Unauthorized",
                          "message": "Invalid or missing X-API-KEY"
                        }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

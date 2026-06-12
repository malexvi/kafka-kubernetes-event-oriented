package com.sfr.sfr_orchestrator_api.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String headerId = request.getHeader("X-Correlation-ID");
        String correlationId = (headerId != null && !headerId.isBlank() ? headerId : UUID.randomUUID().toString());

        MDC.put("correlationId", correlationId);

        request.setAttribute("correlationId", UUID.fromString(correlationId));

        response.setHeader("X-Correlation-ID", correlationId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.clear();
    }
}
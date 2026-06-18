package com.notify.shared.infrastructure.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class RateLimitFilter implements Filter {

    static final int DEFAULT_LIMIT = 5;
    static final long DEFAULT_WINDOW_NANOS = 1_000_000_000L;

    private final Map<String, RateLimiterState> limiterStates = new ConcurrentHashMap<>();

    public void reset() {
        limiterStates.clear();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && (path.contains("/auth/login") || path.contains("/auth/register"))) {
            String key = "auth";
            long now = System.nanoTime();

            RateLimiterState state = limiterStates.computeIfAbsent(key, k -> new RateLimiterState());

            synchronized (state) {
                while (!state.timestamps.isEmpty() && (now - state.timestamps.peekFirst()) > DEFAULT_WINDOW_NANOS) {
                    state.timestamps.pollFirst();
                }

                if (state.timestamps.size() >= DEFAULT_LIMIT) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter()
                            .write("""
                                    {"type":"/errors/rate-limit","title":"Too Many Requests","status":429,"detail":"Too many requests. Please try again later."}
                                    """);
                    return;
                }

                state.timestamps.addLast(now);
            }
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private static class RateLimiterState {
        final Deque<Long> timestamps = new ConcurrentLinkedDeque<>();
    }
}

package ynu.edu.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiterGatewayFilter implements GlobalFilter, Ordered {

    // 每个路径的请求计数器
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    // 每2秒允许的最大请求数
    private static final int MAX_REQUESTS_PER_PERIOD = 5;

    // 时间窗口（毫秒）
    private static final long TIME_WINDOW_MS = 2000;

    // 记录窗口开始时间
    private final Map<String, Long> windowStartTimes = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // 获取当前时间
        long currentTime = System.currentTimeMillis();

        // 获取或初始化该路径的窗口开始时间
        windowStartTimes.putIfAbsent(path, currentTime);
        long windowStart = windowStartTimes.get(path);

        // 检查是否需要重置窗口
        if (currentTime - windowStart > TIME_WINDOW_MS) {
            windowStartTimes.put(path, currentTime);
            requestCounts.put(path, new AtomicInteger(0));
        }

        // 获取当前计数
        requestCounts.putIfAbsent(path, new AtomicInteger(0));
        AtomicInteger count = requestCounts.get(path);

        // 检查是否超过限制
        if (count.incrementAndGet() > MAX_REQUESTS_PER_PERIOD) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return response.setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2; // 在认证过滤器之前执行
    }
}

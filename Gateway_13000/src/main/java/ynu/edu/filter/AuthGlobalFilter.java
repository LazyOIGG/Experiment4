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

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 模拟有效的 Token
    private static final String VALID_TOKEN = "my-token-123";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 1. 获取请求头中的 Token
        String token = request.getHeaders().getFirst("Authorization");

        // 2. 检查路径是否需要认证（登录接口不需要认证）
        String path = request.getURI().getPath();
        if (path.contains("/login")) {
            return chain.filter(exchange);
        }

        // 3. 验证 Token
        if (token == null || token.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        if (!token.equals(VALID_TOKEN)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        // 4. Token 验证通过，继续执行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 过滤器优先级，数值越小优先级越高
        return -1;
    }
}

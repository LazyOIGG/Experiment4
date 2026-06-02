# Experiment4 — Spring Cloud 微服务 API 网关项目

基于 Spring Cloud Gateway 的微服务 API 网关项目，演示动态路由、负载均衡、全局过滤器、熔断降级、限流策略等网关核心功能的实战应用。

## 技术栈

| 组件                          | 版本                            |
|-----------------------------|-------------------------------|
| Java                        | 17                            |
| Spring Boot                 | 4.0.3                         |
| Spring Cloud                | 2025.1.1                      |
| Spring Cloud Gateway        | (managed by Spring Cloud BOM) |
| Spring Cloud Netflix Eureka | —                             |
| Spring Cloud OpenFeign      | —                             |
| Resilience4j                | (managed by Spring Cloud BOM) |
| JMeter                      | 5.6.3                         |

## 模块架构

```
Experiment4
├── Service_Eureka_19000/19001/19002  ← 注册中心集群 (3节点)
├── Provider_15001/15002/15003        ← 服务提供者 'provider-service' (3实例)
├── Public/                           ← 共享模块 (实体类 + Feign接口)
├── Consumer_11001/                   ← 服务消费者 (含Resilience4j)
├── Consumer_11002/                   ← 服务消费者 (无韧性配置)
├── Gateway_13000/                    ← ★ API 网关 (核心模块)
└── 测试计划-实验四.jmx               ← JMeter 测试计划
```

## Gateway_13000 — 配置详情

### 依赖

```xml
spring-cloud-starter-gateway-server-webflux           <!-- Gateway 网关 -->
spring-cloud-starter-circuitbreaker-reactor-resilience4j <!-- 熔断器支持 -->
spring-cloud-starter-netflix-eureka-client             <!-- Eureka 客户端 -->
spring-boot-starter-aop                                <!-- AOP 支持 -->
```

### 动态路由配置

| 路由 ID                  | URI                     | Predicates          | Filters                       |
|------------------------|-------------------------|---------------------|-------------------------------|
| consumer-service-route | `lb://consumer-service` | `Path=/consumer/**` | StripPrefix=1, CircuitBreaker |
| provider-service-route | `lb://provider-service` | `Path=/provider/**` | StripPrefix=1, CircuitBreaker |

> `lb://` 前缀表示使用 Eureka 服务发现进行负载均衡。
> `StripPrefix=1` 去掉路径第一段（如 `/consumer`、`/provider`），保留后续路径转发给目标服务。

### 负载均衡配置

采用**随机策略**（RandomLoadBalancer），配置类位于 `config/LoadBalancerConfig.java`。

```java
@LoadBalancerClients({
    @LoadBalancerClient(name = "provider-service", configuration = LoadBalancerConfig.class),
    @LoadBalancerClient(name = "consumer-service", configuration = LoadBalancerConfig.class)
})
```

| 服务               | 实例数               | 策略 |
|------------------|-------------------|----|
| provider-service | 15001/15002/15003 | 随机 |
| consumer-service | 11001/11002       | 随机 |

### 全局认证过滤器

过滤器类：`filter/AuthGlobalFilter.java`

| 条件                        | 结果                  |
|---------------------------|---------------------|
| 请求路径包含 `/login`           | 放行，不验证 Token        |
| 没有 Token                  | 返回 401 Unauthorized |
| Token 错误                  | 返回 403 Forbidden    |
| Token 正确 (`my-token-123`) | 放行，继续转发             |

### 全局跨域配置

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins: "*"
      allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
      allowedHeaders: "*"
      allowCredentials: false
      maxAge: 3600
```

### 容错机制配置

#### 熔断器 CircuitBreaker

| 实例              | 失败率阈值 | 滑动窗口 | 等待时间 | 半开许可 |
|-----------------|-------|------|------|------|
| gateway-breaker | 50%   | 10次  | 10s  | 3    |

> 当下游服务失败率超过 50%，断路器打开，请求转发到 `/fallback` 降级接口。

#### 限流器 RateLimiter

| 时间窗口 | 每窗口请求数 | 超时策略 |
|------|--------|------|
| 2s   | 5      | 立即拒绝 |

> 超过每 2 秒 5 个请求的限制，返回 429 Too Many Requests。

### 降级处理

降级接口：`controller/FallbackController.java`

```json
{
    "code": 503,
    "message": "服务暂时不可用，已触发熔断降级",
    "data": null
}
```

## JMeter 测试计划

测试计划文件：**`测试计划-实验四.jmx`**

### 线程组概览

| 线程组         | 测试目标       | 并发策略      | 预期结果                    |
|-------------|------------|-----------|-------------------------|
| **1-熔断器测试** | Gateway熔断器 | 30线程×10循环 | 下游服务异常时触发熔断降级           |
| **2-限流器测试** | Gateway限流器 | 20线程集合点×1 | 超过5个请求返回429             |
| **3-过滤器测试** | 认证过滤器      | 10线程×1    | 无Token返回401，有Token返回200 |

### 运行前提

1. 启动 Eureka 注册中心集群 (19000/19001/19002)
2. 启动 Provider 服务实例 (15001/15002/15003)
3. 启动 Consumer_11001 (11001)
4. 启动 Gateway_13000 (13000)
5. 使用 JMeter 打开 `测试计划-实验四.jmx`
6. 分别运行各线程组，查看**查看结果树**和**聚合报告**

## API 访问示例

### 通过 Gateway 访问 Provider

```bash
# 正确 Token
curl -H "Authorization: my-token-123" http://localhost:13000/provider/user/getUserById/1

# 无 Token（返回 401）
curl http://localhost:13000/provider/user/getUserById/1

# 错误 Token（返回 403）
curl -H "Authorization: wrong-token" http://localhost:13000/provider/user/getUserById/1
```

### 负载均衡验证

多次请求同一接口，观察返回结果中端口号标识变化：

```bash
curl -H "Authorization: my-token-123" http://localhost:13000/provider/user/getUserById/1
```

返回结果随机出现 `小明-from 15001`、`小明-from 15002` 或 `小明-from 15003`。

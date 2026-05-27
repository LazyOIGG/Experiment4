package ynu.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import ynu.edu.config.LoadBalancerConfig;

@SpringBootApplication
@LoadBalancerClients({
        @LoadBalancerClient(name = "provider-service", configuration = LoadBalancerConfig.class),
        @LoadBalancerClient(name = "consumer-service", configuration = LoadBalancerConfig.class)
})
public class Gateway13000Application {
    public static void main(String[] args) {
        SpringApplication.run(Gateway13000Application.class, args);
    }
}
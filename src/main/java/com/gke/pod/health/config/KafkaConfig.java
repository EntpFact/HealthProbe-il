package com.gke.pod.health.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Bean
    public HealthIndicator kafkaHealthIndicator() {
        return new HealthIndicator() {
            @Override
            public Health health() {
                try {
                    Properties properties = new Properties();
                    properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
                    AdminClient adminClient = AdminClient.create(properties);
                    KafkaFuture<Health> future = adminClient.describeCluster().controller().thenApply(controller -> {
                        return Health.up().withDetail("Kafka status:","Kafka Service is running").build();

                    });
                    future.get(5, TimeUnit.MILLISECONDS);
                    adminClient.close();
                    return Health.up().withDetail("Kafka Status:","Kafka Health is up").build();
                } catch (Exception e) {
                    return Health.down().withException(e).build();
                }

            }
        };
    }




}

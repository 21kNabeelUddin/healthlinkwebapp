package com.healthlink;

import com.healthlink.config.TestSearchConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestSearchConfig.class)
public abstract class AbstractIntegrationTest {

        // Testcontainers are managed by JUnit lifecycle - no need to close manually
        @SuppressWarnings("resource")
        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.1-alpine")
                        .withDatabaseName("healthlink_test")
                        .withUsername("test")
                        .withPassword("test");

        @SuppressWarnings("resource")
        static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.4.0-alpine"))
                        .withExposedPorts(6379);

        @SuppressWarnings("resource")
        static final GenericContainer<?> elasticsearch = new GenericContainer<>(
                        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.2.1"))
                        .withExposedPorts(9200)
                        .withEnv("discovery.type", "single-node")
                        .withEnv("xpack.security.enabled", "false");

        static {
                postgres.start();
                redis.start();
                elasticsearch.start();
        }

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);

                registry.add("spring.data.redis.host", redis::getHost);
                registry.add("spring.data.redis.port", redis::getFirstMappedPort);

                registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHost() + ":" + elasticsearch.getMappedPort(9200));
        }
}

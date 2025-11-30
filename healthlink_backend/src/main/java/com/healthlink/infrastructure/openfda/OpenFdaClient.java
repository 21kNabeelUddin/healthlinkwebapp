package com.healthlink.infrastructure.openfda;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component
public class OpenFdaClient {

    private final WebClient webClient;

    public OpenFdaClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.fda.gov/drug/interaction.json")
                .build();
    }

    @Cacheable(value = "drugInteractions", key = "#drugA + ':' + #drugB")
    public Optional<JsonNode> checkInteraction(String drugA, String drugB) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("search", String.format("drug1:%s+AND+drug2:%s", drugA, drugB))
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.isError(), this::mapError)
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> Mono.empty())
                .blockOptional();
    }

    private Mono<? extends Throwable> mapError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .map(body -> new IllegalStateException("OpenFDA error status=" + response.statusCode() + " body=" + body));
    }
}

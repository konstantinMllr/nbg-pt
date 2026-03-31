package nbgpt.service;

import nbgpt.dto.ChatRequest;
import nbgpt.dto.ChatResponse;
import nbgpt.dto.EmbeddingRequest;
import nbgpt.dto.EmbeddingResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@Service
public class LlamaClient {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${llama.embedding.url}")
    private String embeddingUrl;

    @Value("${llama.chat.url}")
    private String chatUrl;

    @Value("${llama.model.embedding}")
    private String embeddingModel;

    @Value("${llama.model.chat}")
    private String chatModel;

    public LlamaClient(RestTemplate restTemplate, WebClient.Builder webClientBuilder) {
        this.restTemplate = restTemplate;
        this.webClient = webClientBuilder.build();
    }

    public List<Double> getEmbedding(String text) {
        EmbeddingRequest request = new EmbeddingRequest(embeddingModel, text);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

        EmbeddingResponse response = restTemplate.postForObject(embeddingUrl, entity, EmbeddingResponse.class);
        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            return response.getData().get(0).getEmbedding();
        }
        return Collections.emptyList();
    }

    public String getChatCompletion(String systemPrompt, String userMessage) {
        ChatRequest request = new ChatRequest(chatModel, List.of(
                new ChatRequest.Message("system", systemPrompt),
                new ChatRequest.Message("user", userMessage)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        ChatResponse response = restTemplate.postForObject(chatUrl, entity, ChatResponse.class);
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        return "";
    }

    public Flux<String> getChatCompletionStream(String systemPrompt, String userMessage) {
        ChatRequest request = new ChatRequest(chatModel, List.of(
                new ChatRequest.Message("system", systemPrompt),
                new ChatRequest.Message("user", userMessage)
        ), true);

        return webClient.post()
                .uri(chatUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> !chunk.trim().equals("[DONE]"))
                .map(chunk -> {
                    try {
                        ChatResponse response = objectMapper.readValue(chunk, ChatResponse.class);
                        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                            ChatResponse.Choice choice = response.getChoices().get(0);
                            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                return choice.getDelta().getContent();
                            } else if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                                return choice.getMessage().getContent();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Ignore Parse Error: " + chunk);
                    }
                    return "";
                })
                .filter(content -> !content.isEmpty());
    }
}

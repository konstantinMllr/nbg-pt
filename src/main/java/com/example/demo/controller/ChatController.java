package com.example.demo.controller;

import com.example.demo.service.GraphService;
import com.example.demo.service.LlamaClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final LlamaClient llamaClient;
    private final GraphService graphService;

    public ChatController(LlamaClient llamaClient, GraphService graphService) {
        this.llamaClient = llamaClient;
        this.graphService = graphService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        String userQuestion = request.get("question");
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return Flux.just(Map.of("text", "..."));
        }

        List<Double> vector = llamaClient.getEmbedding(userQuestion);

        if (vector == null || vector.isEmpty()) {
            return Flux.just(Map.of("text", "Fehler beim Generieren der Embeddings"));
        }

        String context = graphService.searchContext(vector);

        String systemPrompt = "Du bist ein Assistent für offene Daten der Stadt Nürnberg. " +
                "Nutze NUR den folgenden Kontext, um die Frage zu beantworten:\n" +
                context;

        return llamaClient.getChatCompletionStream(systemPrompt, userQuestion)
                .map(text -> Map.of("text", text));
    }
}

package nbgpt.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nbgpt.service.GraphService;
import nbgpt.service.LlamaClient;
import reactor.core.publisher.Flux;

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

        String systemPrompt = "Du bist ein präziser und hilfreicher Assistent für offene Daten der Stadt Nürnberg.\n" +
                "Regeln:\n" +
                "1. Beantworte NUR die konkret gestellte Frage.\n" +
                "2. Fasse dich kurz und nenne keine unnötigen Details (etwa Datensätze, Wichtigkeit oder Relevanz-Scores), wenn nicht ausdrücklich danach gefragt wurde.\n" +
                "3. Beachte: 'Bildung, Kultur und Sport' ist EINE einzige zusammenhängende Kategorie. Erfinde oder trenne keine Kategorienamen.\n" +
                "4. Nutze AUSSCHLIESSLICH den folgenden Kontext, um die Frage zu beantworten:\n" +
                "5. Beantworte nur fragen über den Datenbestand der Stadt Nürnberg bzw des Freistaats Bayern\n\n" +

                context;

        return llamaClient.getChatCompletionStream(systemPrompt, userQuestion)
                .map(text -> Map.of("text", text));
    }
}

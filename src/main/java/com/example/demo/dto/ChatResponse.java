package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {
    private List<Choice> choices;

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
        private Message delta;

        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }

        public Message getDelta() { return delta; }
        public void setDelta(Message delta) { this.delta = delta; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String content;
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

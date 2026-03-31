package com.example.demo.dto;

import java.util.List;

public class EmbeddingRequest {
    private String model;
    private String input;

    public EmbeddingRequest(String model, String input) {
        this.model = model;
        this.input = input;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}


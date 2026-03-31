package com.example.demo.dto;

import java.util.List;

public class EmbeddingResponse {
    private List<Data> data;

    public List<Data> getData() { return data; }
    public void setData(List<Data> data) { this.data = data; }

    public static class Data {
        private List<Double> embedding;
        public List<Double> getEmbedding() { return embedding; }
        public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
    }
}


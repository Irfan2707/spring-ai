package com.spring.ai.demo.service;

import org.springframework.ai.document.Document;

import java.util.List;

// DataLoaderService.java
public interface DataLoaderService {

    List<Document> loadFromJson(byte[] fileBytes, String... jsonKeys);

    List<Document> loadFromText(byte[] fileBytes);
    List<Document> loadFromHtml(byte[] fileBytes);
    List<Document> loadFromPdfByPage(byte[] fileBytes);
    List<Document> loadFromPdfByParagraph(byte[] fileBytes);
}
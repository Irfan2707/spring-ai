package com.spring.ai.demo.service;

import java.util.List;
import org.springframework.ai.document.Document;

// DataLoaderService.java
public interface DataLoaderService {

  List<Document> loadFromJson(byte[] fileBytes, String... jsonKeys);

  List<Document> loadFromText(byte[] fileBytes);

  List<Document> loadFromHtml(byte[] fileBytes);

  List<Document> loadFromPdfByPage(byte[] fileBytes);

  List<Document> loadFromPdfByParagraph(byte[] fileBytes);

  // Route to the correct reader based on type
  // type values: json | text | html | pdf-page | pdf-paragraph
  List<Document> loadDocumentsByType(byte[] fileBytes, String type);
}

package com.spring.ai.demo.service;

import java.util.List;
import org.springframework.ai.document.Document;

// DataTransformerService.java
// PURPOSE: Defines all transformation operations on raw Documents
// Transformers run AFTER reading and BEFORE writing to vector store
public interface DataTransformerService {

  // Splits large documents into smaller chunks by character count
  List<Document> splitByText(List<Document> documents);

  // Splits documents by token count (more accurate for LLM context windows)
  List<Document> splitByToken(List<Document> documents);

  // Reformats document content into a standard template string
  List<Document> formatContent(List<Document> documents);

  // Uses LLM to extract keywords and adds them as metadata
  List<Document> enrichWithKeywords(List<Document> documents);

  // Uses LLM to generate summaries and adds them as metadata
  List<Document> enrichWithSummary(List<Document> documents);

  // Runs all transformers in the correct sequence in one shot
  List<Document> applyAll(List<Document> documents);
}

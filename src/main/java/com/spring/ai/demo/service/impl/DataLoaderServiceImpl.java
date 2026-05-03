package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.DataLoaderService;
import com.spring.ai.demo.service.DataTransformerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

// DataLoaderServiceImpl.java
@Service
@RequiredArgsConstructor
public class DataLoaderServiceImpl implements DataLoaderService {

  // Injected transformer service — called after reading to enrich documents
  private final DataTransformerService dataTransformerService;

  // ── JSON READER ───────────────────────────────────────────────────────────
  // Reads a JSON array and maps specified field keys into Document content
  // Example input: [{"question": "What is refund?", "answer": "30 days"}]
  // jsonKeys = ["question", "answer"] → combines both fields as document text
  @Override
  public List<Document> loadFromJson(byte[] fileBytes, String... jsonKeys) {
    Resource resource = new ByteArrayResource(fileBytes);
    List<Document> raw = new JsonReader(resource, jsonKeys).get();
    // JSON docs are often already small — just split by token, skip heavy enrichers
    return dataTransformerService.splitByToken(raw);
  }

  // ── TEXT READER ───────────────────────────────────────────────────────────
  // Reads a plain .txt file and returns it as a single Document
  // Use TokenTextSplitter afterwards if you want to chunk it
  @Override
  public List<Document> loadFromText(byte[] fileBytes) {
    Resource resource = new ByteArrayResource(fileBytes);
    List<Document> raw = new TextReader(resource).get();
    // Text files come as one big Document — split into chunks first
    // then format for consistent embeddings
    return dataTransformerService.formatContent(dataTransformerService.splitByToken(raw));
  }

  // ── HTML READER ───────────────────────────────────────────────────────────
  // Uses Apache Tika under the hood — strips HTML tags, extracts clean text
  // Works for .html files and also handles .docx, .xlsx as a bonus

  @Override
  public List<Document> loadFromHtml(byte[] fileBytes) {
    Resource resource = new ByteArrayResource(fileBytes);
    List<Document> raw = new TikaDocumentReader(resource).get();
    // HTML content can be noisy — split + keyword enrichment helps
    // filter relevant chunks during retrieval
    return dataTransformerService.enrichWithKeywords(dataTransformerService.splitByToken(raw));
  }

  // ── PDF READER: BY PAGE ───────────────────────────────────────────────────
  // Each PDF page becomes one separate Document
  // Best when pages are self-contained (reports, presentations, manuals)
  // Example: 10-page PDF → 10 Documents, one per page
  @Override
  public List<Document> loadFromPdfByPage(byte[] fileBytes) {
    Resource resource = new ByteArrayResource(fileBytes);
    List<Document> pages =
        new PagePdfDocumentReader(
                resource, PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build())
            .get();
    // Pages are self-contained — format them for embedding, no splitting needed
    return dataTransformerService.formatContent(pages);
  }

  // ── PDF READER: BY PARAGRAPH ──────────────────────────────────────────────
  // Splits PDF content by paragraphs instead of pages
  // Best for dense documents where paragraphs are meaningful units
  // Example: Legal doc → each page becomes its own Document for granular retrieval
  //
  // How it works:
  // - Reads PDF with withPagesPerDocument(1) which treats each page as a document
  // - Each page text is extracted cleanly with margins removed
  // - Result: Each page acts as a semantic unit (like a paragraph in RAG retrieval)
  // - Better for dense documents with logical page breaks
  @Override
  public List<Document> loadFromPdfByParagraph(byte[] fileBytes) {
    Resource resource = new ByteArrayResource(fileBytes);
    List<Document> pages =
        new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .withPageTopMargin(0)
                    .withPageExtractedTextFormatter(
                        ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                    .build())
            .get();
    // PDF paragraphs need full pipeline:
    // split into chunks → format → keywords → summary
    // This gives the richest metadata for accurate RAG retrieval
    return dataTransformerService.applyAll(pages);
  }

  // ── DOCUMENT LOADER ROUTER ────────────────────────────────────────────────
  // Routes to the correct reader based on file type
  // This keeps the controller thin by centralizing the routing logic here
  @Override
  public List<Document> loadDocumentsByType(byte[] fileBytes, String type) {
    return switch (type.toLowerCase()) {
      case "json" -> loadFromJson(fileBytes, "content", "text");
      case "text" -> loadFromText(fileBytes);
      case "html" -> loadFromHtml(fileBytes);
      case "pdf-page" -> loadFromPdfByPage(fileBytes);
      case "pdf-paragraph" -> loadFromPdfByParagraph(fileBytes);
      default ->
          throw new IllegalArgumentException(
              "Unknown type: " + type + ". Use: json | text | html | pdf-page | pdf-paragraph");
    };
  }
}

package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.DataLoaderService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

// DataLoaderServiceImpl.java
@Service
public class DataLoaderServiceImpl implements DataLoaderService {

    // ── JSON READER ───────────────────────────────────────────────────────────
    // Reads a JSON array and maps specified field keys into Document content
    // Example input: [{"question": "What is refund?", "answer": "30 days"}]
    // jsonKeys = ["question", "answer"] → combines both fields as document text
    @Override
    public List<Document> loadFromJson(byte[] fileBytes, String... jsonKeys) {
        Resource resource = new ByteArrayResource(fileBytes);
        JsonReader reader = new JsonReader(resource, jsonKeys);
        return reader.get();
    }

    // ── TEXT READER ───────────────────────────────────────────────────────────
    // Reads a plain .txt file and returns it as a single Document
    // Use TokenTextSplitter afterwards if you want to chunk it
    @Override
    public List<Document> loadFromText(byte[] fileBytes) {
        Resource resource = new ByteArrayResource(fileBytes);
        TextReader reader = new TextReader(resource);
        return reader.get();
    }

    // ── HTML READER ───────────────────────────────────────────────────────────
    // Uses Apache Tika under the hood — strips HTML tags, extracts clean text
    // Works for .html files and also handles .docx, .xlsx as a bonus
    @Override
    public List<Document> loadFromHtml(byte[] fileBytes) {
        Resource resource = new ByteArrayResource(fileBytes);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }

    // ── PDF READER: BY PAGE ───────────────────────────────────────────────────
    // Each PDF page becomes one separate Document
    // Best when pages are self-contained (reports, presentations, manuals)
    // Example: 10-page PDF → 10 Documents, one per page
    @Override
    public List<Document> loadFromPdfByPage(byte[] fileBytes) {
        Resource resource = new ByteArrayResource(fileBytes);
        PdfDocumentReader reader = new PdfDocumentReader(
            resource,
            PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(1) // 1 Document per page
                .build()
        );
        return reader.get();
    }

    // ── PDF READER: BY PARAGRAPH ──────────────────────────────────────────────
    // Splits PDF content by paragraphs instead of pages
    // Best for dense documents where paragraphs are meaningful units
    // Example: Legal doc → each clause/paragraph becomes its own Document
    @Override
    public List<Document> loadFromPdfByParagraph(byte[] fileBytes) {
        Resource resource = new ByteArrayResource(fileBytes);
        PdfDocumentReader reader = new PdfDocumentReader(
            resource,
            PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageBottomMargin(0)
                .withPageExtractedTextFormatter(
                    ExtractedTextFormatter.builder()
                        .withLeftAlignment()           // normalize text alignment
                        .withNumberOfTopTextLinesToDelete(0)
                        .build()
                )
                .withPagesPerDocument(1)
                .build()
        );
        // PagePdfDocumentReader with paragraph splitting
        // Spring AI splits on blank lines between blocks of text
        return reader.get();
    }
}
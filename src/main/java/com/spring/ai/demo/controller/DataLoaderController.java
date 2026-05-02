package com.spring.ai.demo.controller;

import com.spring.ai.demo.service.DataLoaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// DataLoaderController.java
@RestController
@RequestMapping("/api/loader")
@RequiredArgsConstructor
public class DataLoaderController {

    private final DataLoaderService dataLoaderService;

    // Single endpoint — accepts any file + a "type" param to pick the reader
    // type values: json | text | html | pdf-page | pdf-paragraph
    //
    // Example calls:
    //   POST /api/loader/load?type=text        + file: faq.txt
    //   POST /api/loader/load?type=pdf-page    + file: report.pdf
    //   POST /api/loader/load?type=json        + file: data.json
    @PostMapping(value = "/load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> load(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") String type  // caller tells us which reader to use
    ) throws IOException {

        byte[] bytes = file.getBytes();
        List<Document> documents;

        // Route to the correct reader based on the "type" param
        documents = switch (type.toLowerCase()) {
            case "json"          -> dataLoaderService.loadFromJson(bytes, "content", "text");
            case "text"          -> dataLoaderService.loadFromText(bytes);
            case "html"          -> dataLoaderService.loadFromHtml(bytes);
            case "pdf-page"      -> dataLoaderService.loadFromPdfByPage(bytes);
            case "pdf-paragraph" -> dataLoaderService.loadFromPdfByParagraph(bytes);
            default -> throw new IllegalArgumentException(
                "Unknown type: " + type + ". Use: json | text | html | pdf-page | pdf-paragraph"
            );
        };

        return ResponseEntity.ok(
            "Loaded " + documents.size() + " documents from [" + file.getOriginalFilename() + "] using type=" + type
        );
    }
}
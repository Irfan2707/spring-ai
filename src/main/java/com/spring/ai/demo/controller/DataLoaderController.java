package com.spring.ai.demo.controller;

import com.spring.ai.demo.service.DataLoaderService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// DataLoaderController.java
@RestController
@RequestMapping("/api/loader")
@RequiredArgsConstructor
@Tag(
    name = "Document Loader",
    description =
        "Endpoints for loading and processing various document formats (JSON, Text, HTML, PDF)")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Document Loader API",
            version = "1.0",
            description =
                "APIs for loading documents from different file types and converting them to Spring AI Document format for RAG workflows"))
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
  @Operation(
      summary = "Load and process documents from various file formats",
      description =
          "Accepts a file and file type, then uses the appropriate reader to load the document into Spring AI Document format. Supports JSON, Text, HTML, PDF (by page), and PDF (by paragraph).")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Documents successfully loaded",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid file type or malformed file"),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during file processing")
      })
  public ResponseEntity<Map<String, Object>> load(
      @RequestPart("file") MultipartFile file,
      @Parameter(
              description =
                  "File type and loading strategy. Valid values: 'json' (JSON array), 'text' (plain text), 'html' (HTML content), 'pdf-page' (each page as document), 'pdf-paragraph' (paragraph-level splitting)")
          @RequestParam("type")
          String type)
      throws IOException {
    byte[] bytes = file.getBytes();
    List<Document> documents = dataLoaderService.loadDocumentsByType(bytes, type);

    // Build response with document details
    Map<String, Object> response = new HashMap<>();
    response.put("fileName", file.getOriginalFilename());
    response.put("fileType", type);
    response.put("totalDocuments", documents.size());

    // Loop through documents and add details
    List<Map<String, Object>> docList = new ArrayList<>();
    for (int i = 0; i < documents.size(); i++) {
      Document doc = documents.get(i);
      Map<String, Object> docMap = new HashMap<>();
      docMap.put("documentIndex", i + 1);
      docMap.put("content", doc.getText());
      docMap.put("contentLength", doc.getText() != null ? doc.getText().length() : 0);
      docMap.put("metadata", doc.getMetadata());
      docList.add(docMap);
    }
    response.put("documents", docList);

    return ResponseEntity.ok(response);
  }
}

package com.spring.ai.demo.controller;

import com.spring.ai.demo.service.RagVectorDatabaseService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/rag")
@Tag(
    name = "RAG Vector Database",
    description =
        "Endpoints for managing RAG (Retrieval-Augmented Generation) vector database operations")
@OpenAPIDefinition(
    info =
        @Info(
            title = "RAG Vector Database API",
            version = "1.0",
            description =
                "APIs for storing and retrieving data from vector database for RAG workflows"))
public class RagVectorDataController {

  private final RagVectorDatabaseService ragVectorDatabaseService;

  public RagVectorDataController(RagVectorDatabaseService ragVectorDatabaseService) {
    this.ragVectorDatabaseService = ragVectorDatabaseService;
  }

  @PutMapping("/store-chunks-to-db")
  @Operation(
      summary = "Store data chunks into vector database",
      description = "Accepts a list of strings and stores them as documents in the vector database")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Data successfully stored in vector database"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void storeDataIntoVectorDatabase(@RequestBody List<String> companyDataList) {
    ragVectorDatabaseService.storeDataIntoVectorDatabase(companyDataList);
  }

  @GetMapping("/similarity-search")
  @Operation(
      summary = "Search similar documents from vector database",
      description =
          "Performs a similarity search on the vector database and returns relevant documents along with AI-generated response")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search successful, returns AI-generated response",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public String similaritySearch(
      @Parameter(description = "Search query string") @RequestParam String query) {
    return ragVectorDatabaseService.similaritySearchFromDatabase(query);
  }

  @GetMapping("/rag-with-advisor")
  @Operation(
      summary = "Retrieve documents using RetrievalAugmentationAdvisor",
      description =
          "Performs RAG (Retrieval-Augmented Generation) using RetrievalAugmentationAdvisor which searches the vector database for relevant documents and provides them as context to the AI model for generating answers")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "RAG retrieval successful, returns AI-generated response with context from vector store",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public String retrievalAugmentedGenerationWithAdvisor(
      @Parameter(description = "Search query string") @RequestParam String query) {
    return ragVectorDatabaseService.retrievalAugmentationAdvisor(query);
  }

  @GetMapping("/rag-with-query-augmentor")
  @Operation(
      summary = "Retrieve documents with Query Augmentation",
      description =
          "Performs advanced RAG using QueryAugmentor paired with RetrievalAugmentationAdvisor. The QueryAugmentor transforms the user's query using the RAG system prompt template before retrieving documents, enabling better context matching and more relevant results")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "RAG retrieval successful with query augmentation, returns AI-generated response with enhanced context",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public String retrievalAugmentationAdvisorWithQueryAugmentor(
      @Parameter(description = "Search query string") @RequestParam String query) {
    return ragVectorDatabaseService.retrievalAugmentationAdvisorWithQueryAugmentor(query);
  }
}

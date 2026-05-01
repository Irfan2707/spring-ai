package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.RagVectorDatabaseService;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class RagVectorDatabaseServiceImpl implements RagVectorDatabaseService {

  @Value("classpath:/prompts/rag-system-prompt.st")
  private Resource systemMessage;

  @Value("classpath:/prompts/rag-user-prompt.st")
  private Resource userMessage;

  private final VectorStore vectorStore;
  private final ChatClient groqChatClient;

  public RagVectorDatabaseServiceImpl(VectorStore vectorStore,@Qualifier("groqChatClientWithoutChatMemory") ChatClient groqChatClient) {
    this.vectorStore = vectorStore;
    this.groqChatClient = groqChatClient;
  }

  @Override
  public void storeDataIntoVectorDatabase(List<String> companyDataList) {

    List<Document> documentList = companyDataList.stream().map(Document::new).toList();
    this.vectorStore.add(documentList);
  }

  @Override
  public String similaritySearchFromDatabase(String query) {

    SearchRequest searchRequest = SearchRequest
            .builder()
                    .topK(5)
                            .similarityThreshold(.5)
            .query(query)
                                    .build();

    List<Document> documentList = vectorStore.similaritySearch(searchRequest);
    List<@Nullable String> convertedDocuments = documentList.stream().map(Document::getText).toList();
    String finalQuery = String.join("\n", convertedDocuments);



           return groqChatClient
            .prompt()
            .system(promptSystemSpec ->promptSystemSpec.text(this.systemMessage).param("doucments",finalQuery) )
            .user(promptUserSpec -> promptUserSpec.text(this.userMessage).param("query", query))
            .call()
            .content();
  }


}

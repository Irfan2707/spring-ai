package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.RagVectorDatabaseService;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
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

  public RagVectorDatabaseServiceImpl(
      VectorStore vectorStore,
      @Qualifier("groqChatClientWithoutChatMemory") ChatClient groqChatClient) {
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

    //    We can also use QuestionAnswerAdvisor instead of creating context by ourselves.Spring ai
    // will do it for ourselvees.
    //    SearchRequest searchRequest = SearchRequest
    //            .builder()
    //                    .topK(5)
    //                            .similarityThreshold(.5)
    //            .query(query)
    //                                    .build();
    //
    //    List<Document> documentList = vectorStore.similaritySearch(searchRequest);
    //    List<@Nullable String> convertedDocuments =
    // documentList.stream().map(Document::getText).toList();
    //    String finalQuery = String.join("\n", convertedDocuments);

    // QuestionAnswerAdvisor automatically searches the vector store for similar documents
    // based on the query and provides them as context to the LLM. This enables RAG
    // (Retrieval-Augmented Generation)
    // without manually building the context. The advisor will:
    // 1. Search the vector database for top-3 most similar documents (topK=3)
    // 2. Filter results with similarity threshold of 0.5
    // 3. Pass the retrieved documents as context to the prompt
    // 4. Allow the LLM to generate answers based on the retrieved context
    return groqChatClient
        .prompt()
        .advisors(
            QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(3).similarityThreshold(0.5).build())
                .build())
        .user(promptUserSpec -> promptUserSpec.text(this.userMessage).param("query", query))
        .call()
        .content();
  }
}

package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.RagVectorDatabaseService;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagVectorDatabaseServiceImpl implements RagVectorDatabaseService {

  private final VectorStore vectorStore;

  public RagVectorDatabaseServiceImpl(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @Override
  public void storeDataIntoVectorDatabase(List<String> companyDataList) {

    List<Document> documentList = companyDataList.stream().map(Document::new).toList();
    this.vectorStore.add(documentList);
  }
}

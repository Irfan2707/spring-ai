package com.spring.ai.demo.service;

import org.springframework.ai.document.Document;

import java.util.List;

public interface RagVectorDatabaseService {

  void storeDataIntoVectorDatabase(List<String> companyDataList);

 String similaritySearchFromDatabase(String query);
}

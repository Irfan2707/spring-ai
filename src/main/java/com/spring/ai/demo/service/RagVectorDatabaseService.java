package com.spring.ai.demo.service;

import java.util.List;

public interface RagVectorDatabaseService {

  void storeDataIntoVectorDatabase(List<String> companyDataList);

  String similaritySearchFromDatabase(String query);
}

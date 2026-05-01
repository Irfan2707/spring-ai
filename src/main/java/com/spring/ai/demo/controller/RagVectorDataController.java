package com.spring.ai.demo.controller;

import com.spring.ai.demo.service.RagVectorDatabaseService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rag")
public class RagVectorDataController {

  private final RagVectorDatabaseService ragVectorDatabaseService;

  public RagVectorDataController(RagVectorDatabaseService ragVectorDatabaseService) {
    this.ragVectorDatabaseService = ragVectorDatabaseService;
  }

  @PutMapping("/store-chunks-to-db")
  public void storeDataIntoVectorDatabase(@RequestBody List<String> companyDataList) {
    ragVectorDatabaseService.storeDataIntoVectorDatabase(companyDataList);
  }
}

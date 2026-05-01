package com.spring.ai.demo;

import com.spring.ai.demo.helper.Helper;
import com.spring.ai.demo.service.ChatService;
import com.spring.ai.demo.service.RagVectorDatabaseService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoProjectApplicationTests {

  @Test
  void contextLoads() {}

  @Autowired private ChatService chatService;

  @Test
  void templateUsingFluentApiCase() {

    var result = chatService.templateUsingFluentApi();

    System.out.println(result);
  }

  @Test
  void testResumeUseCase() {

    Map<String, Object> input =
        Map.of(
            "experience", 2,
            "skills", "Java, Spring Boot, Microservices",
            "projects", "Built scalable backend systems");

    var result = chatService.chatTemplate("resume", input);

    result.forEach(System.out::println);
  }

  @Autowired private RagVectorDatabaseService ragVectorDatabaseService;

  @Test
  void saveDataToVectorDatabase() {

    System.out.println("Saving data to database");
    ragVectorDatabaseService.storeDataIntoVectorDatabase(Helper.getData());
    System.out.println("Data saved successfully to database");
  }
}

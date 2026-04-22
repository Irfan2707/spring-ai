package com.spring.ai.demo;

import com.spring.ai.demo.service.ChatService;
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
}

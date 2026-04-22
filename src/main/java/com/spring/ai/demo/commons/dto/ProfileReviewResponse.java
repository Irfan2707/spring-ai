package com.spring.ai.demo.commons.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProfileReviewResponse {

  private String skill;
  private String rating;
  private String remarks;
}

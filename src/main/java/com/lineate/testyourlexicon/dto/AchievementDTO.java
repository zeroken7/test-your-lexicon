package com.lineate.testyourlexicon.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AchievementDTO {

  private String name;
  private String description;

}

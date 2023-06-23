package com.lineate.testyourlexicon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lineate.testyourlexicon.models.Question;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class StepDto {

  @JsonProperty("game_id")
  private Long gameId;
  @JsonProperty("question")
  Question question;

}

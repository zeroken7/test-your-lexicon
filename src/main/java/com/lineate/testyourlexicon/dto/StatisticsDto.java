package com.lineate.testyourlexicon.dto;


import lombok.Data;

@Data
public class StatisticsDto {

  private int questionsAnswered;
  private int correctlyAnswered;
  private String wordWithMostHits;
  private String wordWithMostMisses;

}

package com.lineate.testyourlexicon.achievements;

import com.lineate.testyourlexicon.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
public class StepsAnsweredAchievement implements Achievement {

  private final QuestionRepository questionRepository;
  private String name;
  private int stepsRequired;

  @Override
  public boolean acquireAchievement(long userHash) {
    return questionRepository
        .countNumberOfQuestionsAnsweredByUser(userHash) >= stepsRequired;
  }

  @Override
  public String getName() {
    return name;
  }

}

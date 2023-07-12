package com.lineate.testyourlexicon.achievements;

import com.lineate.testyourlexicon.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
public class GamesPlayedAchievement implements Achievement {

  private final GameRepository gameRepository;
  private String name;
  private int gamesRequired;

  @Override
  public boolean acquireAchievement(long userHash) {
    return gameRepository.countGameByUserHash(userHash) >= gamesRequired;
  }

  @Override
  public String getName() {
    return name;
  }

}

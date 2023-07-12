package com.lineate.testyourlexicon.config;

import com.lineate.testyourlexicon.achievements.AchievementManager;
import com.lineate.testyourlexicon.achievements.GamesPlayedAchievement;
import com.lineate.testyourlexicon.achievements.StepsAnsweredAchievement;
import com.lineate.testyourlexicon.repositories.AchievementRepository;
import com.lineate.testyourlexicon.repositories.GameRepository;
import com.lineate.testyourlexicon.repositories.QuestionRepository;
import com.lineate.testyourlexicon.repositories.UserRepository;
import com.lineate.testyourlexicon.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AchievementConfig {

  private final QuestionRepository questionRepository;
  private final GameRepository gameRepository;
  private final UserRepository userRepository;
  private final AchievementRepository achievementRepository;
  private final AuthenticationService authenticationService;

  private StepsAnsweredAchievement generalStepsAnsweredAchievement(String name,
                                                                   int steps) {
    StepsAnsweredAchievement result = new StepsAnsweredAchievement(questionRepository);
    result.setName(name);
    result.setStepsRequired(steps);
    return result;
  }

  private void addStepsAnsweredAchievements(AchievementManager achievementManager) {
    achievementManager.addAchievement(generalStepsAnsweredAchievement("Linguistic Beginner", 10));
    achievementManager.addAchievement(generalStepsAnsweredAchievement("Language Interpreter", 200));
    achievementManager.addAchievement(generalStepsAnsweredAchievement("Translation Maestro", 1000));
    achievementManager.addAchievement(generalStepsAnsweredAchievement("Global Ambassador", 10000));
    achievementManager.addAchievement(generalStepsAnsweredAchievement("Supreme Polyglot", 50000));
  }

  private GamesPlayedAchievement generalGamesPlayedAchievement(String name,
                                                                 int games) {
    GamesPlayedAchievement result = new GamesPlayedAchievement(gameRepository);
    result.setName(name);
    result.setGamesRequired(games);
    return result;
  }

  private void addGamesPlayedAchievement(AchievementManager achievementManager) {
    achievementManager.addAchievement(generalGamesPlayedAchievement("Newbie Player", 1));
    achievementManager.addAchievement(generalGamesPlayedAchievement("Dedicated Translator", 25));
    achievementManager.addAchievement(generalGamesPlayedAchievement("Prodigy Gamer", 500));
    achievementManager.addAchievement(generalGamesPlayedAchievement("Elite Champion", 2500));
    achievementManager.addAchievement(generalGamesPlayedAchievement("Grandmaster Linguistic",
                                                                    10000));
  }

  @Bean
  public AchievementManager achievementManager() {
    AchievementManager achievementManager = new AchievementManager(userRepository,
        achievementRepository,
        authenticationService);
    addStepsAnsweredAchievements(achievementManager);
    addGamesPlayedAchievement(achievementManager);
    return achievementManager;
  }


}

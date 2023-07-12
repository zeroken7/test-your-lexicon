package com.lineate.testyourlexicon.achievements;

import com.lineate.testyourlexicon.entities.AchievementEntity;
import com.lineate.testyourlexicon.entities.User;
import com.lineate.testyourlexicon.repositories.AchievementRepository;
import com.lineate.testyourlexicon.repositories.UserRepository;
import com.lineate.testyourlexicon.services.AuthenticationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AchievementManager {

  private final UserRepository userRepository;
  private final AchievementRepository achievementRepository;
  private final AuthenticationService authenticationService;
  private final List<Achievement> achievements = new ArrayList<>();

  public void checkAchievements(Long userHash) {
    if (!authenticationService.isAuthenticated()) {
      return;
    }
    User user = authenticationService.getAuthenticatedUser().get();
    achievements.forEach(achievement -> {
      boolean acquired = achievement.acquireAchievement(userHash);
      if (acquired) {
        giveUserAchievement(user, achievement.getName());
      }
    });
  }

  private void giveUserAchievement(User user, String name) {
    AchievementEntity achievementEntity
        = achievementRepository.findAchievementEntityByName(name)
          .get();
    user.addAchievement(achievementEntity);
    userRepository.save(user);
  }

  public void addAchievement(Achievement achievement) {
    achievements.add(achievement);
  }

}

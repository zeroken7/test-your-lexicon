package com.lineate.testyourlexicon.unittests;

import com.lineate.testyourlexicon.achievements.GamesPlayedAchievement;
import com.lineate.testyourlexicon.achievements.StepsAnsweredAchievement;
import com.lineate.testyourlexicon.repositories.GameRepository;
import com.lineate.testyourlexicon.repositories.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

public class AchievementsTest {

  private GameRepository gameRepository;
  private QuestionRepository questionRepository;
  private GamesPlayedAchievement gamesPlayedAchievement;
  private StepsAnsweredAchievement stepsAnsweredAchievement;

  @BeforeEach
  public void setUp() {
    gameRepository = mock(GameRepository.class);
    questionRepository = mock(QuestionRepository.class);
    gamesPlayedAchievement = new GamesPlayedAchievement(gameRepository);
    stepsAnsweredAchievement = new StepsAnsweredAchievement(questionRepository);
  }

  @Test
  public void testGamesPlayedAchievement() {
    gamesPlayedAchievement.setGamesRequired(5);
    Long userHash = 12345L;
    when(gameRepository.countGameByUserHash(userHash))
      .thenReturn(1).thenReturn(3).thenReturn(5).thenReturn(10);
    // The first two shouldn't give achievement
    assertThat(gamesPlayedAchievement.acquireAchievement(userHash)).isFalse();
    assertThat(gamesPlayedAchievement.acquireAchievement(userHash)).isFalse();
    // This two should be acquired
    assertThat(gamesPlayedAchievement.acquireAchievement(userHash)).isTrue();
    assertThat(gamesPlayedAchievement.acquireAchievement(userHash)).isTrue();
  }

  @Test
  public void test() {
    stepsAnsweredAchievement.setStepsRequired(5);
    Long userHash = 12345L;
    when(questionRepository.countNumberOfQuestionsAnsweredByUser(userHash))
      .thenReturn(1).thenReturn(3).thenReturn(5).thenReturn(10);
    // The first two shouldn't give achievement
    assertThat(stepsAnsweredAchievement.acquireAchievement(userHash)).isFalse();
    assertThat(stepsAnsweredAchievement.acquireAchievement(userHash)).isFalse();
    // This two should be acquired
    assertThat(stepsAnsweredAchievement.acquireAchievement(userHash)).isTrue();
    assertThat(stepsAnsweredAchievement.acquireAchievement(userHash)).isTrue();
  }
}

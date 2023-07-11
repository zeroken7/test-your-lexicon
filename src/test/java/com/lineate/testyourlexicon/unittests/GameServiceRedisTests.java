package com.lineate.testyourlexicon.unittests;

import com.lineate.testyourlexicon.config.TestRedisConfiguration;
import com.lineate.testyourlexicon.repositories.*;
import com.lineate.testyourlexicon.services.GameService;
import com.lineate.testyourlexicon.services.TranslationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import redis.clients.jedis.Jedis;

import static org.mockito.Mockito.mock;

@SpringBootTest
public class GameServiceRedisTests {


  private GameConfigurationRepository gameConfigurationRepository;
  private GameService gameService;
  private TranslationService translationService;
  private TranslationRepository translationRepository;
  private GameRepository gameRepository;
  private QuestionRepository questionRepository;
  private UserStatisticsRepository userStatisticsRepository;
  @Autowired
  private Jedis jedis;

  @BeforeEach
  public void setUp() {
    translationService = mock(TranslationService.class);
    translationRepository = mock(TranslationRepository.class);
    questionRepository = mock(QuestionRepository.class);
    gameRepository = mock(GameRepository.class);
    gameConfigurationRepository = mock(GameConfigurationRepository.class);
    userStatisticsRepository = mock(UserStatisticsRepository.class);
    gameService = new GameService(gameConfigurationRepository, translationService,
      translationRepository, gameRepository, questionRepository, userStatisticsRepository,
      jedis);
  }

  @Test
  public void testTimeOut() throws InterruptedException {
    long gameId = 12345L;
    gameService.startStepTimeout(gameId, 5);
    Assertions.assertDoesNotThrow(() -> gameService.checkTimeout(gameId));
    Thread.sleep(3000);
    Assertions.assertDoesNotThrow(() -> gameService.checkTimeout(gameId));
    Thread.sleep(2000);
    Assertions.assertThrows(Exception.class, () -> gameService.checkTimeout(gameId));
  }

}

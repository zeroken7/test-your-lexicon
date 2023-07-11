package com.lineate.testyourlexicon.services;

import com.lineate.testyourlexicon.dto.*;
import com.lineate.testyourlexicon.entities.Game;
import com.lineate.testyourlexicon.entities.GameConfiguration;
import com.lineate.testyourlexicon.entities.QuestionEntity;
import com.lineate.testyourlexicon.exceptions.GeneralMessageException;
import com.lineate.testyourlexicon.models.Question;
import com.lineate.testyourlexicon.models.UserStatistics;
import com.lineate.testyourlexicon.repositories.*;
import com.lineate.testyourlexicon.util.GameMapper;
import com.lineate.testyourlexicon.util.GameUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {
  private final GameConfigurationRepository gameConfigurationRepository;
  private final TranslationService translationService;
  private final TranslationRepository translationRepository;
  private final GameRepository gameRepository;
  private final QuestionRepository questionRepository;
  private final UserStatisticsRepository userStatisticsRepository;
  private final Jedis jedis;

  public GameConfigurationDto configure(GameConfigurationDto gameConfigurationDto, Long userHash) {
    GameConfiguration gameConfiguration =
        GameMapper.gameConfigurationDtoToGameConfiguration(gameConfigurationDto);
    List<String> supportedLanguages = translationService.supportedLanguages();
    if (!supportedLanguages.contains(gameConfigurationDto.getTranslateFrom())
        || !supportedLanguages.contains(gameConfigurationDto.getTranslateTo())) {
      throw new IllegalArgumentException("unsupported language");
    }

    gameConfiguration.setUserHash(userHash);
    gameConfigurationRepository.save(gameConfiguration);

    return GameMapper.gameConfigurationToGameConfigurationDto(gameConfiguration);
  }

  public GameConfigurationDto userConfiguration(Long userHash) {
    GameConfiguration gameConfiguration = gameConfigurationRepository.findById(userHash)
        .orElseGet(() -> {
          GameConfiguration defaultConfiguration =
              GameUtil.defaultGameConfiguration();
          defaultConfiguration.setUserHash(userHash);
          gameConfigurationRepository.save(defaultConfiguration);
          return defaultConfiguration;
        });
    return GameMapper.gameConfigurationToGameConfigurationDto(gameConfiguration);
  }

  public SupportedLanguagesDto supportedLanguages() {
    return new SupportedLanguagesDto(translationService.supportedLanguages());
  }

  public GameInitializedDto initGameForUser(Long userHash) {
    checkIfGameActiveForUser(userHash);
    Game game = new Game();
    game.setUserHash(userHash);
    game.setStepsLeft(userConfiguration(userHash).getNumberOfSteps());
    gameRepository.save(game);
    log.info("Initialized new game for user {'user_hash': {}, 'game_id': {}}",
        userHash, game.getGameId());
    return new GameInitializedDto(game.getGameId());
  }

  public Question generateRandomQuestion(GameConfiguration gameConfiguration, Game game) {
    String translateFrom = gameConfiguration.getTranslateFrom();
    String translateTo = gameConfiguration.getTranslateTo();
    int answerOptionsCount = gameConfiguration.getAnswerCount();
    Question question = null;
    do {
      question = translationService.getRandomQuestion(translateFrom,
        translateTo, answerOptionsCount);
    } while (questionRepository.existsByGameAndTranslationId(game, question.getTranslationId()));
    return question;
  }

  public QuestionEntity saveQuestionInDatabase(Question question, Game game) {
    QuestionEntity questionEntity = new QuestionEntity();
    questionEntity.setGuessed(false);
    questionEntity.setTranslationId(question.getTranslationId());
    questionEntity.setGame(game);
    return questionRepository.save(questionEntity);
  }

  public Game updateGameCurrentQuestion(Game game, QuestionEntity questionEntity) {
    game.setCurrentQuestionId(questionEntity.getId());
    return gameRepository.save(game);
  }

  public void startStepTimeout(long gameId, int time) {
    String redisKey = String.format("game_id:step:%d", gameId);
    jedis.setex(redisKey, time, "");
  }

  public void checkTimeout(long gameId) {
    String redisKey = String.format("game_id:step:%d", gameId);
    if (!jedis.exists(redisKey)) {
      throw new GeneralMessageException("Step timed out!");
    }
  }

  public StepDto userActiveGameNextStep(Long userHash, long gameId) {
    Game game = validateGameForUser(userHash, gameId);

    GameConfiguration gameConfiguration =
        GameMapper.gameConfigurationDtoToGameConfiguration(userConfiguration(userHash));
    Question question = generateRandomQuestion(gameConfiguration, game);
    QuestionEntity questionEntity = saveQuestionInDatabase(question, game);
    final Game updatedGame = updateGameCurrentQuestion(game, questionEntity);
    log.info("Generated step {'user_hash': {}, 'game_id': {}, 'question_id': {}}",
        userHash, updatedGame.getGameId(), questionEntity.getId());
    startStepTimeout(gameId, gameConfiguration.getStepTimeInSeconds());
    return new StepDto(question, gameId);
  }

  public QuestionEntity getQuestion(Long questionId) {
    if (questionId == null) {
      throw new GeneralMessageException("Couldn't not find active question");
    }
    Optional<QuestionEntity> questionEntity =
        questionRepository.findById(questionId);
    if (questionEntity.isEmpty()) {
      throw new GeneralMessageException("Invalid question id");
    }
    return questionEntity.get();
  }

  @Transactional
  public AnswerResponseDto userActiveGameAnswer(Long userHash,
                                   AnswerRequestDto answerRequestDto,
                                   long gameId) {
    Game game = validateGameForUser(userHash, gameId);
    // Check if game isn't timed out
    checkTimeout(gameId);
    QuestionEntity questionEntity = getQuestion(game.getCurrentQuestionId());

    // Get correct answer
    Long translationId = questionEntity.getTranslationId();
    String language = userConfiguration(userHash).getTranslateTo();
    String correctAnswer =
         translationRepository.languageDefinitionGivenIdAndLanguage(translationId, language);

    // Check if users answer is the same as correct one
    boolean guessed = correctAnswer.equalsIgnoreCase(answerRequestDto.getAnswer());
    if (guessed) {
      questionEntity.setGuessed(true);
      questionRepository.save(questionEntity);
    }
    game.setCurrentQuestionId(null);
    game.setStepsLeft(game.getStepsLeft() - 1);
    gameRepository.save(game);

    updateStatistics(userHash, translationId, guessed);
    log.info("User answered a step {'user_hash': {}, 'game_id': {}, 'question': {}}",
        userHash, game.getGameId(), questionEntity.getId());
    return AnswerResponseDto.builder()
      .guessed(guessed)
      .correctAnswer(correctAnswer)
      .userAnswer(answerRequestDto.getAnswer())
      .build();
  }

  public void updateStatistics(Long userHash, Long translationId, boolean guessed) {
    UserStatistics userStatistics = userStatisticsRepository
        .findById(userHash).orElseGet(() ->
          new UserStatistics(userHash)
        );
    userStatistics.setQuestionsAnswered(userStatistics.getQuestionsAnswered() + 1);
    if (guessed) {
      userStatistics.setCorrectlyAnswered(userStatistics.getCorrectlyAnswered() + 1);
      userStatistics.hitWord(translationId);
    } else {
      userStatistics.missWord(translationId);
    }
    userStatisticsRepository.save(userStatistics);
  }

  public StatisticsDto getUserStatistics(Long userHash) {
    UserStatistics userStatistics = userStatisticsRepository
        .findById(userHash).orElseGet(() ->
          new UserStatistics(userHash)
        );
    StatisticsDto statisticsDto = new StatisticsDto();
    statisticsDto.setQuestionsAnswered(userStatistics.getQuestionsAnswered());
    statisticsDto.setCorrectlyAnswered(userStatistics.getCorrectlyAnswered());
    Long mostHitsId = Collections.max(userStatistics.getHits().entrySet(),
        Map.Entry.comparingByValue()).getKey();
    Long mostMissesId = Collections.max(userStatistics.getMisses().entrySet(),
      Map.Entry.comparingByValue()).getKey();
    statisticsDto.setWordWithMostHits(
        translationRepository.languageDefinitionGivenIdAndLanguage(mostHitsId, "english"));
    statisticsDto.setWordWithMostMisses(
        translationRepository.languageDefinitionGivenIdAndLanguage(mostMissesId, "english"));
    return statisticsDto;
  }

  private void checkIfGameActiveForUser(Long userHash) {
    if (gameRepository.getUserActiveGame(userHash).isPresent()) {
      throw new GeneralMessageException("A game is already started for the user");
    }
  }

  private Game validateGameForUser(Long userHash, Long gameId) {
    Game game = gameRepository.findById(gameId).orElseThrow(() -> {
      log.info("User tried to access game with invalid id "
          + "{'user_hash': {}, 'game_id': {}}", userHash, gameId);
      return new GeneralMessageException("Game id not valid");
    });
    if (!game.getUserHash().equals(userHash)) {
      log.info("User tried to access game that didn't belong to him "
          + "{'user_hash': {}, 'game_id': {}}", userHash, gameId);
      throw new GeneralMessageException("Given game doesn't belong to the user");
    }
    if (game.getStepsLeft() <= 0) {
      log.info("User tried to access game that was finished "
          + "{'user_hash': {}, 'game_id': {}}", userHash, gameId);
      throw new GeneralMessageException("Game has already finished");
    }
    return game;
  }
}

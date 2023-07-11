package com.lineate.testyourlexicon.unittests;


import com.lineate.testyourlexicon.dto.*;
import com.lineate.testyourlexicon.entities.Game;
import com.lineate.testyourlexicon.entities.QuestionEntity;
import com.lineate.testyourlexicon.models.Question;
import com.lineate.testyourlexicon.repositories.*;
import com.lineate.testyourlexicon.services.GameService;
import com.lineate.testyourlexicon.services.TranslationService;
import com.lineate.testyourlexicon.util.GameUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameServiceTests {

  private GameConfigurationRepository gameConfigurationRepository;
  private GameService gameService;
  private TranslationService translationService;
  private TranslationRepository translationRepository;
  private GameRepository gameRepository;
  private QuestionRepository questionRepository;
  private UserStatisticsRepository userStatisticsRepository;
  private Jedis jedis;

  @BeforeEach
  public void setUp() {
    translationService = mock(TranslationService.class);
    translationRepository = mock(TranslationRepository.class);
    questionRepository = mock(QuestionRepository.class);
    gameRepository = mock(GameRepository.class);
    jedis = mock(Jedis.class);
    gameConfigurationRepository = mock(GameConfigurationRepository.class);
    userStatisticsRepository = mock(UserStatisticsRepository.class);
    gameService = new GameService(gameConfigurationRepository, translationService,
      translationRepository, gameRepository, questionRepository, userStatisticsRepository,
      jedis);
  }

  @Test
  public void whenGetRandomQuestion_GetQuestion() {
    Question customQuestion =
      new Question(1L, "park", List.of("park", "notPark"));
    when(translationService.getRandomQuestion(any(), any(), anyInt()))
      .thenReturn(customQuestion);
    when(questionRepository.existsByGameAndTranslationId(any(), any()))
      .thenReturn(false);
    Question resultQuestion =
      gameService.generateRandomQuestion(GameUtil.defaultGameConfiguration(), new Game());
    assertThat(resultQuestion.getWord()).isEqualTo(customQuestion.getWord());
  }

  @Test
  public void whenGetRandomQuestion_ThereAreNoDuplicates() {
    Question customQuestion1 =
      new Question(1L, "park", List.of("park", "notPark"));
    Question customQuestion2 =
      new Question(2L, "tree", List.of("tree", "notTree"));
    when(translationService.getRandomQuestion(any(), any(), anyInt()))
      .thenReturn(customQuestion1).thenReturn(customQuestion2);
    when(questionRepository.existsByGameAndTranslationId(any(), any()))
      .thenReturn(true).thenReturn(false);
    Question resultQuestion =
      gameService.generateRandomQuestion(GameUtil.defaultGameConfiguration(), new Game());
    assertThat(resultQuestion.getWord()).isEqualTo(customQuestion2.getWord());
  }

  @Test
  public void whenSavingGameQuestionOnDatabase_ThenReturnCorrespondingQuestionEntity() {
    when(questionRepository.save(any()))
      .thenAnswer(new Answer<QuestionEntity>() {

        @Override
        public QuestionEntity answer(InvocationOnMock invocationOnMock) throws Throwable {
          return (QuestionEntity) invocationOnMock.getArgument(0);
        }
      });
    Question customQuestion =
      new Question(1L, "park", List.of("park", "notPark"));
    Game game = new Game();
    QuestionEntity questionEntity = gameService.saveQuestionInDatabase(customQuestion, game);
    assertThat(questionEntity.getGame()).isEqualTo(game);
    assertThat(questionEntity.getTranslationId()).isEqualTo(customQuestion.getTranslationId());
  }

  @Test
  public void whenUpdatingGameWithNewQuestion_GameStepsLeftDecrement() {
    when(questionRepository.save(any()))
      .thenAnswer(new Answer<Game>() {

        @Override
        public Game answer(InvocationOnMock invocationOnMock) throws Throwable {
          return (Game) invocationOnMock.getArgument(0);
        }
      });
    Game game = new Game();
    game.setStepsLeft(10);
    gameService.updateGameCurrentQuestion(game, new QuestionEntity());
    assertThat(game.getStepsLeft()).isEqualTo(9);
  }

  @Test
  public void whenUserAnswersCorrectly() {
    when(translationRepository.languageDefinitionGivenIdAndLanguage(any(), any()))
      .thenReturn("correct");
    when(jedis.exists(any(String.class))).thenReturn(true);
    Long userHash = 12345L;
    Game game = new Game();
    game.setGameId(1L);
    game.setUserHash(userHash);
    game.setStepsLeft(10);
    game.setCurrentQuestionId(10L);
    when(gameRepository.findById(any()))
      .thenReturn(Optional.of(game));
    when(questionRepository.findById(any()))
      .thenReturn(Optional.of(new QuestionEntity()));
    AnswerRequestDto answerRequestDto = new AnswerRequestDto();
    answerRequestDto.setAnswer("correct");

    AnswerResponseDto answerResponseDto =
         gameService.userActiveGameAnswer(userHash, answerRequestDto, game.getGameId());
    assertThat(answerResponseDto.isGuessed()).isTrue();
    assertThat(answerResponseDto.getUserAnswer()).isEqualTo("correct");
    assertThat(answerResponseDto.getCorrectAnswer()).isEqualTo("correct");
  }


  @Test
  public void whenUserAnswersIncorrectly() {
    when(translationRepository.languageDefinitionGivenIdAndLanguage(any(), any()))
      .thenReturn("correct");
    when(jedis.exists(any(String.class))).thenReturn(true);
    Long userHash = 12345L;
    Game game = new Game();
    game.setGameId(1L);
    game.setUserHash(userHash);
    game.setStepsLeft(10);
    game.setCurrentQuestionId(10L);
    when(gameRepository.findById(any()))
      .thenReturn(Optional.of(game));
    when(questionRepository.findById(any()))
      .thenReturn(Optional.of(new QuestionEntity()));
    AnswerRequestDto answerRequestDto = new AnswerRequestDto();
    answerRequestDto.setAnswer("incorrect");

    AnswerResponseDto answerResponseDto =
      gameService.userActiveGameAnswer(userHash, answerRequestDto, game.getGameId());
    assertThat(answerResponseDto.isGuessed()).isFalse();
    assertThat(answerResponseDto.getUserAnswer()).isEqualTo("incorrect");
    assertThat(answerResponseDto.getCorrectAnswer()).isEqualTo("correct");
  }

  @Test
  public void whenInitGameForUser_ThenGameIsCreatedInDatabase() {
    gameService.initGameForUser(12345L);
    verify(gameRepository).save(any());
  }

  @Test
  public void whenUserHasAlreadyStartedGame_ExceptionIsThrown() {
    Long userHash = 12345L;
    when(gameRepository.getUserActiveGame(userHash)).thenReturn(Optional.of(new Game()));
    assertThatException().isThrownBy(() -> gameService.initGameForUser(userHash));
  }


  @Test
  public void supportedLanguagesContainsDefaultOnes() {
    when(translationService.supportedLanguages())
      .thenReturn(Arrays.asList(GameUtil.DEFAULT_TRANSLATE_FROM_LANGUAGE,
                                GameUtil.DEFAULT_TRANSLATE_TO_LANGUAGE));
    SupportedLanguagesDto supportedLanguagesDto
      = gameService.supportedLanguages();
    List<String> languages = supportedLanguagesDto.languages();
    assertThat(languages.contains(GameUtil.DEFAULT_TRANSLATE_FROM_LANGUAGE))
      .isTrue();
    assertThat(languages.contains(GameUtil.DEFAULT_TRANSLATE_TO_LANGUAGE))
      .isTrue();
  }

  @Test
  public void whenUserHasDefaultConfiguration_WeGetSameConfigurationBack() {
    GameConfigurationDto gameConfigurationDto =
      gameService.userConfiguration(12345L);

    assertThat(gameConfigurationDto.getTranslateFrom())
      .isEqualTo(GameUtil.DEFAULT_TRANSLATE_FROM_LANGUAGE);
    assertThat(gameConfigurationDto.getTranslateTo())
      .isEqualTo(GameUtil.DEFAULT_TRANSLATE_TO_LANGUAGE);
    assertThat(gameConfigurationDto.getNumberOfSteps())
      .isEqualTo(GameUtil.DEFAULT_STEP_COUNT);
    assertThat(gameConfigurationDto.getStepTimeInSeconds())
      .isEqualTo(GameUtil.DEFAULT_STEP_TIME);
    assertThat(gameConfigurationDto.getAnswerCount())
      .isEqualTo(GameUtil.DEFAULT_ANSWER_COUNT);
  }
}

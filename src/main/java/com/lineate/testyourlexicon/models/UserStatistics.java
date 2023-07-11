package com.lineate.testyourlexicon.models;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("UserStatistics")
@Getter
@Setter
public class UserStatistics {

  private Long id;
  private int questionsAnswered = 0;
  private int correctlyAnswered = 0;
  private Map<Long, Integer> hits = new HashMap<>();
  private Map<Long, Integer> misses = new HashMap<>();

  public void hitWord(long translationId) {
    hits.put(translationId, hits.getOrDefault(translationId, 0) + 1);
  }

  public void missWord(long translationId) {
    misses.put(translationId, misses.getOrDefault(translationId, 0) + 1);
  }

  public UserStatistics(Long id) {
    this.id = id;
  }
}

package com.lineate.testyourlexicon.achievements;

public interface Achievement {

  boolean acquireAchievement(long userHash);

  String getName();

}

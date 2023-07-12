package com.lineate.testyourlexicon.repositories;

import com.lineate.testyourlexicon.entities.AchievementEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<AchievementEntity, Integer> {

  Optional<AchievementEntity> findAchievementEntityByName(String name);

}

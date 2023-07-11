package com.lineate.testyourlexicon.repositories;

import com.lineate.testyourlexicon.models.UserStatistics;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatisticsRepository extends CrudRepository<UserStatistics, Long> {

}

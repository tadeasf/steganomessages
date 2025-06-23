package com.tadeasfort.steganomessages.repository;

import com.tadeasfort.steganomessages.model.User;
import com.tadeasfort.steganomessages.model.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    Optional<UserStatistics> findByUser(User user);

    boolean existsByUser(User user);
}
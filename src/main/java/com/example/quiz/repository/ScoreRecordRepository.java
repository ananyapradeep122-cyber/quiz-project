package com.example.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quiz.model.ScoreRecord;

public interface ScoreRecordRepository extends JpaRepository<ScoreRecord, Long> {
    List<ScoreRecord> findTop10ByOrderByScoreDescCreatedAtAsc();
    long countByScoreGreaterThan(int score);
    java.util.Optional<ScoreRecord> findByShareToken(String shareToken);
}
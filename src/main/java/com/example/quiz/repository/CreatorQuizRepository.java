package com.example.quiz.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.quiz.model.CreatorQuiz;

@Repository
public interface CreatorQuizRepository extends JpaRepository<CreatorQuiz, Long> {
    Optional<CreatorQuiz> findByShareToken(String shareToken);
}
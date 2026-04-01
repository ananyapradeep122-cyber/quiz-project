package com.example.quiz.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.quiz.model.CreatorQuiz;
import com.example.quiz.model.Question;
import com.example.quiz.model.ScoreRecord;
import com.example.quiz.repository.CreatorQuizRepository;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.ScoreRecordRepository;

@Service
public class QuizService {

    @Autowired
    private QuestionRepository repo;

    @Autowired
    private ScoreRecordRepository scoreRepo;

    @Autowired
    private CreatorQuizRepository creatorQuizRepo;

    public List<Question> getAllQuestions() {
        return repo.findAll();
    }

    public CreatorQuiz saveCreatorQuiz(String creatorName, List<String> answers) {
        CreatorQuiz quiz = new CreatorQuiz();
        quiz.setCreatorName(creatorName);
        quiz.setAnswers(String.join("\n", answers));
        quiz.setShareToken(UUID.randomUUID().toString());
        quiz.setCreatedAt(LocalDateTime.now());
        return creatorQuizRepo.save(quiz);
    }

    public CreatorQuiz getCreatorQuizByToken(String token) {
        return creatorQuizRepo.findByShareToken(token).orElse(null);
    }

    public int calculateScore(List<String> answers) {
        List<Question> questions = repo.findAll();

        int score = 0;
        for (int i = 0; i < questions.size() && i < answers.size(); i++) {
            if (questions.get(i).getAnswer().equalsIgnoreCase(answers.get(i))) {
                score++;
            }
        }
        return score;
    }

    public int calculateSharedQuizScore(List<String> playerAnswers, String creatorToken) {
        CreatorQuiz creatorQuiz = getCreatorQuizByToken(creatorToken);
        if (creatorQuiz == null) {
            return 0;
        }
        List<String> correctAnswers = Arrays.stream(creatorQuiz.getAnswers().split("\n"))
                .map(String::trim)
                .toList();

        int score = 0;
        for (int i = 0; i < correctAnswers.size() && i < playerAnswers.size(); i++) {
            if (correctAnswers.get(i).equalsIgnoreCase(playerAnswers.get(i))) {
                score++;
            }
        }
        return score;
    }

    public ScoreRecord saveScore(String playerName, int score) {
        ScoreRecord record = new ScoreRecord();
        record.setPlayerName(playerName);
        record.setScore(score);
        record.setShareToken(UUID.randomUUID().toString());
        record.setCreatedAt(LocalDateTime.now());
        return scoreRepo.save(record);
    }

    public ScoreRecord getScoreByToken(String token) {
        return scoreRepo.findByShareToken(token).orElse(null);
    }

    public List<ScoreRecord> getLeaderboard() {
        return scoreRepo.findTop10ByOrderByScoreDescCreatedAtAsc();
    }

    public int getRank(int score) {
        return (int) scoreRepo.countByScoreGreaterThan(score) + 1;
    }
}
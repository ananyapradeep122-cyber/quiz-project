package com.example.quiz.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.quiz.model.CreatorQuiz;
import com.example.quiz.model.Question;
import com.example.quiz.model.ScoreRecord;
import com.example.quiz.service.QuizService;

@RestController
@CrossOrigin
public class QuizController {

    @Autowired
    private QuizService service;

    @GetMapping("/questions")
    public List<Question> getAllQuestions() {
        return service.getAllQuestions();
    }

    @PostMapping("/submit")
    public Map<String, Object> submitQuiz(@RequestBody SubmitQuizRequest request) {
        int score = service.calculateScore(request.getAnswers());
        var saved = service.saveScore(request.getName(), score);
        int rank = service.getRank(score);

        Map<String, Object> response = new HashMap<>();
        response.put("name", request.getName());
        response.put("score", score);
        response.put("total", service.getAllQuestions().size());
        response.put("rank", rank);
        response.put("shareUrl", "/share/" + saved.getShareToken());
        response.put("leaderboard", service.getLeaderboard().stream().map(this::toRecordMap).collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> leaderboard() {
        return service.getLeaderboard().stream().map(this::toRecordMap).collect(Collectors.toList());
    }

    @PostMapping("/creator")
    public Map<String, Object> createQuiz(@RequestBody CreateQuizRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creator name is required");
        }
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answers are required");
        }

        CreatorQuiz created = service.saveCreatorQuiz(request.getName().trim(), request.getAnswers());
        Map<String, Object> response = new HashMap<>();
        response.put("creatorName", created.getCreatorName());
        response.put("shareUrl", "/creator/view/" + created.getShareToken());
        response.put("questionCount", service.getAllQuestions().size());
        return response;
    }

    @GetMapping("/creator/view/{token}")
    public ResponseEntity<Resource> getCreatorView(@PathVariable String token) {
        CreatorQuiz quiz = service.getCreatorQuizByToken(token);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator quiz not found");
        }
        Resource resource = new ClassPathResource("static/index.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(resource);
    }

    @GetMapping("/creator/{token}")
    public Map<String, Object> getCreatorQuiz(@PathVariable String token) {
        CreatorQuiz quiz = service.getCreatorQuizByToken(token);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator quiz not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("creatorName", quiz.getCreatorName());
        response.put("questionCount", service.getAllQuestions().size());
        response.put("shareUrl", "/creator/view/" + quiz.getShareToken());
        response.put("questions", service.getAllQuestions().stream().map(this::toQuestionMap).collect(Collectors.toList()));
        return response;
    }

    @PostMapping("/play/{token}")
    public Map<String, Object> playSharedQuiz(@PathVariable String token, @RequestBody PlayQuizRequest request) {
        CreatorQuiz quiz = service.getCreatorQuizByToken(token);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator quiz not found");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player name is required");
        }

        int correct = service.calculateSharedQuizScore(request.getAnswers(), token);
        ScoreRecord saved = service.saveScore(request.getName().trim(), correct);
        int rank = service.getRank(saved.getScore());

        Map<String, Object> response = new HashMap<>();
        response.put("playerName", saved.getPlayerName());
        response.put("creatorName", quiz.getCreatorName());
        response.put("score", saved.getScore());
        response.put("total", service.getAllQuestions().size());
        response.put("rank", rank);
        response.put("shareUrl", "/share/" + saved.getShareToken());
        response.put("leaderboard", service.getLeaderboard().stream().map(this::toRecordMap).collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/share/{token}")
    public Map<String, Object> getSharedResult(@PathVariable String token) {
        var record = service.getScoreByToken(token);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Share link not found");
        }

        int rank = service.getRank(record.getScore());
        Map<String, Object> response = new HashMap<>();
        response.put("name", record.getPlayerName());
        response.put("score", record.getScore());
        response.put("total", service.getAllQuestions().size());
        response.put("rank", rank);
        response.put("shareUrl", "/share/" + record.getShareToken());
        response.put("leaderboard", service.getLeaderboard().stream().map(this::toRecordMap).collect(Collectors.toList()));
        return response;
    }

    private Map<String, Object> toQuestionMap(Question question) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", question.getId());
        row.put("question", question.getQuestion());
        row.put("option1", question.getOption1());
        row.put("option2", question.getOption2());
        row.put("option3", question.getOption3());
        row.put("option4", question.getOption4());
        return row;
    }

    private Map<String, Object> toRecordMap(ScoreRecord record) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", record.getPlayerName());
        row.put("score", record.getScore());
        row.put("playedAt", record.getCreatedAt().toString());
        return row;
    }

    public static class SubmitQuizRequest {
        private String name;
        private List<String> answers;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getAnswers() {
            return answers;
        }

        public void setAnswers(List<String> answers) {
            this.answers = answers;
        }
    }

    public static class CreateQuizRequest {
        private String name;
        private List<String> answers;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getAnswers() {
            return answers;
        }

        public void setAnswers(List<String> answers) {
            this.answers = answers;
        }
    }

    public static class PlayQuizRequest {
        private String name;
        private List<String> answers;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getAnswers() {
            return answers;
        }

        public void setAnswers(List<String> answers) {
            this.answers = answers;
        }
    }
}
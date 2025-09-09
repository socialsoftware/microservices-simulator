package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

@Autowired
private QuizService quizService;

@GetMapping(value = "/quizs")
public ResponseEntity<List<QuizDto>> getAllQuizs(
        ) {
        List<QuizDto> result = quizService.getAllQuizs();
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/quizs/{id}")
public ResponseEntity<QuizDto> getQuiz(
        @PathVariable Long id
        ) throws Exception {
        QuizDto result = quizService.getQuiz(id);
        return ResponseEntity.ok(result);
        }

@PostMapping(value = "/quizs")
public ResponseEntity<QuizDto> createQuiz(
        @RequestBody QuizDto quizDto
        ) throws Exception {
        QuizDto result = quizService.createQuiz(quizDto);
        return ResponseEntity.ok(result);
        }

@PutMapping(value = "/quizs/{id}")
public ResponseEntity<QuizDto> updateQuiz(
        @PathVariable Long id,
        @RequestBody QuizDto quizDto
        ) throws Exception {
        QuizDto result = quizService.updateQuiz(id,
        quizDto);
        return ResponseEntity.ok(result);
        }

@DeleteMapping(value = "/quizs/{id}")
public ResponseEntity<Void> deleteQuiz(
        @PathVariable Long id
        ) throws Exception {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok().build();
        }

        }
package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/question")
public class QuestionController {

@Autowired
private QuestionService questionService;

@GetMapping(value = "/questions")
public ResponseEntity<List<QuestionDto>> getAllQuestions(
        ) {
        List<QuestionDto> result = questionService.getAllQuestions();
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/questions/{id}")
public ResponseEntity<QuestionDto> getQuestion(
        @PathVariable Long id
        ) throws Exception {
        QuestionDto result = questionService.getQuestion(id);
        return ResponseEntity.ok(result);
        }

@PostMapping(value = "/questions")
public ResponseEntity<QuestionDto> createQuestion(
        @RequestBody QuestionDto questionDto
        ) throws Exception {
        QuestionDto result = questionService.createQuestion(questionDto);
        return ResponseEntity.ok(result);
        }

@PutMapping(value = "/questions/{id}")
public ResponseEntity<QuestionDto> updateQuestion(
        @PathVariable Long id,
        @RequestBody QuestionDto questionDto
        ) throws Exception {
        QuestionDto result = questionService.updateQuestion(id,
        questionDto);
        return ResponseEntity.ok(result);
        }

@DeleteMapping(value = "/questions/{id}")
public ResponseEntity<Void> deleteQuestion(
        @PathVariable Long id
        ) throws Exception {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok().build();
        }

        }
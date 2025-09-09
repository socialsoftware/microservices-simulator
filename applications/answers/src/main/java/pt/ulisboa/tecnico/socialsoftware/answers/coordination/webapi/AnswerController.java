package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/answer")
public class AnswerController {

@Autowired
private AnswerService answerService;

@GetMapping(value = "/answers")
public ResponseEntity<List<QuizAnswerDto>> getAllAnswers(
        ) {
        List<QuizAnswerDto> result = answerService.getAllAnswers();
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/answers/{id}")
public ResponseEntity<QuizAnswerDto> getAnswer(
        @PathVariable Long id
        ) throws Exception {
        QuizAnswerDto result = answerService.getAnswer(id);
        return ResponseEntity.ok(result);
        }

@PostMapping(value = "/answers")
public ResponseEntity<QuizAnswerDto> createAnswer(
        @RequestBody QuizAnswerDto answerDto
        ) throws Exception {
        QuizAnswerDto result = answerService.createAnswer(answerDto);
        return ResponseEntity.ok(result);
        }

@PutMapping(value = "/answers/{id}")
public ResponseEntity<QuizAnswerDto> updateAnswer(
        @PathVariable Long id,
        @RequestBody QuizAnswerDto answerDto
        ) throws Exception {
        QuizAnswerDto result = answerService.updateAnswer(id,
        answerDto);
        return ResponseEntity.ok(result);
        }

@DeleteMapping(value = "/answers/{id}")
public ResponseEntity<Void> deleteAnswer(
        @PathVariable Long id
        ) throws Exception {
        answerService.deleteAnswer(id);
        return ResponseEntity.ok().build();
        }

        }
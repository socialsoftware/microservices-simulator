package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities;

@RestController
public class QuizController {
    @Autowired
    private QuizFunctionalities quizFunctionalities;

    @PostMapping("/executions/{courseExecutionId}")
    public QuizDto createQuiz(@PathVariable Integer courseExecutionId, @RequestBody QuizDto quizDto) throws Exception {
        return quizFunctionalities.createQuiz(courseExecutionId, quizDto);
    }
    @PostMapping("/quizzes/update")
    public QuizDto updateQuiz(@RequestBody QuizDto quizDto) throws Exception {
        return quizFunctionalities.updateQuiz(quizDto);
    }

    @GetMapping("/quizzes/{quizAggregateId}")
    public QuizDto findQuiz(@PathVariable Integer quizAggregateId) {
        return quizFunctionalities.findQuiz(quizAggregateId);
    }
}

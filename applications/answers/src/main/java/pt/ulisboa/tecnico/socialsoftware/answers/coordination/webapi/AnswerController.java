package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
public class AnswerController {
    @Autowired
    private AnswerFunctionalities answerFunctionalities;

    @PostMapping("/quizzes/{quizAggregateId}/answer")
    public void answerQuestion(@PathVariable Integer quizAggregateId, @RequestParam Integer userAggregateId, @RequestBody AnswerDto questionAnswerDto) throws Exception {
        answerFunctionalities.answerQuestion(quizAggregateId, userAggregateId, questionAnswerDto);
    }
}

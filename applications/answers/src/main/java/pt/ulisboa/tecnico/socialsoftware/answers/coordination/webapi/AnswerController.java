package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

@RestController
public class AnswerController {
    @Autowired
    private AnswerFunctionalities answerFunctionalities;

    @PostMapping("/quizzes/{quizAggregateId}/answer")
    public void answerQuestion(@PathVariable Integer quizAggregateId, @RequestParam Integer userAggregateId, @RequestBody AnswerDto questionAnswerDto) throws Exception {
        answerFunctionalities.answerQuestion(quizAggregateId, userAggregateId, questionAnswerDto);
    }
}

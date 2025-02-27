package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuizAnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;

@RestController
public class QuizAnswerController {

    @Autowired
    private QuizAnswerFunctionalities quizAnswerFunctionalities;

    @PostMapping("/quizzes/{quizAggregateId}/answer")
    public void answerQuestion(@PathVariable Integer quizAggregateId, @RequestParam Integer userAggregateId, @RequestBody QuestionAnswerDto questionAnswerDto) throws Exception {
        quizAnswerFunctionalities.answerQuestion(quizAggregateId, userAggregateId, questionAnswerDto);
    }
}

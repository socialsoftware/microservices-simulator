package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.causal.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.causal.CausalQuestion;

import java.util.List;

@Service
@Profile("tcc")
public class CausalQuestionFactory implements QuestionFactory {

    @Override
    public Question createQuestion(Integer aggregateId, QuestionCourse questionCourse, QuestionDto questionDto, List<QuestionTopic> questionTopics) {
        return new CausalQuestion(aggregateId, questionCourse, questionDto, questionTopics);
    }

    @Override
    public Question createQuestionFromExisting(Question existingQuestion) {
        return new CausalQuestion((CausalQuestion) existingQuestion);
    }

    @Override
    public QuestionDto createQuestionDto(Question question) {
        return new QuestionDto(question);
    }
    
}

package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalQuestion;

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
    
}

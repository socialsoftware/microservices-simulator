package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaUserDto;

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

package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.SagaQuestion;

import java.util.Set;

@Service
@Profile("sagas")
public class SagasQuestionFactory implements QuestionFactory {

    @Override
    public SagaQuestion createQuestion(Integer aggregateId, String title, String content, QuestionCourse questionCourse, Set<Option> options, Set<QuestionTopic> topics) {
        return new SagaQuestion(aggregateId, title, content, questionCourse, options, topics);
    }

    @Override
    public SagaQuestion createQuestionCopy(Question existing) {
        return new SagaQuestion((SagaQuestion) existing);
    }

    @Override
    public QuestionDto createQuestionDto(Question question) {
        return new QuestionDto(question);
    }
}

package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.SagaQuestion;

import java.time.LocalDateTime;

@Service
@Profile("sagas")
public class SagasQuestionFactory implements QuestionFactory {
    @Override
    public SagaQuestion createQuestion(Integer aggregateId, Integer courseAggregateId, String title, String content,
                                       LocalDateTime creationDate) {
        return new SagaQuestion(aggregateId, courseAggregateId, title, content, creationDate);
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

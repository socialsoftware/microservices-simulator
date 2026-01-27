package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuestionDto;

@Service
@Profile("sagas")
public class SagasQuestionFactory implements QuestionFactory {
    @Override
    public Question createQuestion(Integer aggregateId, QuestionDto questionDto) {
        return new SagaQuestion(aggregateId, questionDto);
    }

    @Override
    public Question createQuestionFromExisting(Question existingQuestion) {
        return new SagaQuestion((SagaQuestion) existingQuestion);
    }

    @Override
    public QuestionDto createQuestionDto(Question question) {
        return new SagaQuestionDto(question);
    }
}
package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaAnswer;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaAnswerDto;

@Service
@Profile("sagas")
public class SagasAnswerFactory extends AnswerFactory {
@Override
public Answer createAnswer(Integer aggregateId, AnswerDto answerDto) {
return new SagaAnswer(answerDto);
}

@Override
public Answer createAnswerFromExisting(Answer existingAnswer) {
return new SagaAnswer((SagaAnswer) existingAnswer);
}

@Override
public AnswerDto createAnswerDto(Answer answer) {
return new SagaAnswerDto(answer);
}
}
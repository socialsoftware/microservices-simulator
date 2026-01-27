package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaAnswer;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuestionAnswered;
import java.util.List;

@Service
@Profile("sagas")
public class SagasAnswerFactory implements AnswerFactory {
    @Override
    public Answer createAnswer(Integer aggregateId, AnswerExecution execution, AnswerUser user, AnswerQuiz quiz, AnswerDto answerDto, List<QuestionAnswered> questions) {
        return new SagaAnswer(aggregateId, execution, user, quiz, answerDto, questions);
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
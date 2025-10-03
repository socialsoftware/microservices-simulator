package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class AnswerFactory {

    public Answer createAnswer(Integer aggregateId, AnswerDto answerDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new Answer(
            answerDto.getAnswerDate(),
            answerDto.getCompletedDate(),
            answerDto.getCompleted(),
            answerDto.getQuizAnswerStudent(),
            answerDto.getQuizAnswerExecution(),
            answerDto.getQuestionAnswers(),
            answerDto.getAnsweredQuiz()
        );
    }

    public Answer createAnswerFromExisting(Answer existingAnswer) {
        // Create a copy of the existing aggregate
        if (existingAnswer instanceof Answer) {
            return new Answer((Answer) existingAnswer);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public AnswerDto createAnswerDto(Answer answer) {
        return new AnswerDto((Answer) answer);
    }
}
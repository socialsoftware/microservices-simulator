package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class AnswerFactory {

    public Answer createAnswer(Integer aggregateId, QuizAnswerDto answerDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new QuizAnswer(
            answerDto.getAnswerDate(),
            answerDto.getCompletedDate(),
            answerDto.getCompleted(),
            answerDto.getQuizAnswerStudent(),
            answerDto.getQuizAnswerCourseExecution(),
            answerDto.getQuestionAnswers(),
            answerDto.getAnsweredQuiz()
        );
    }

    public Answer createAnswerFromExisting(Answer existingAnswer) {
        // Create a copy of the existing aggregate
        if (existingAnswer instanceof QuizAnswer) {
            return new QuizAnswer((QuizAnswer) existingAnswer);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public QuizAnswerDto createQuizAnswerDto(Answer answer) {
        return new QuizAnswerDto((QuizAnswer) answer);
    }
}
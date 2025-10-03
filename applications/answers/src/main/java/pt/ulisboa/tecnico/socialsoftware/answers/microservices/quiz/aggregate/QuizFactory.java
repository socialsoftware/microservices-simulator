package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class QuizFactory {

    public Quiz createQuiz(Integer aggregateId, QuizDto quizDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new Quiz(
            quizDto.getTitle(),
            quizDto.getDescription(),
            quizDto.getQuizType(),
            quizDto.getAvailableDate(),
            quizDto.getConclusionDate(),
            quizDto.getNumberOfQuestions(),
            quizDto.getExecution(),
            quizDto.getQuestions(),
            quizDto.getOptions()
        );
    }

    public Quiz createQuizFromExisting(Quiz existingQuiz) {
        // Create a copy of the existing aggregate
        if (existingQuiz instanceof Quiz) {
            return new Quiz((Quiz) existingQuiz);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public QuizDto createQuizDto(Quiz quiz) {
        return new QuizDto((Quiz) quiz);
    }
}
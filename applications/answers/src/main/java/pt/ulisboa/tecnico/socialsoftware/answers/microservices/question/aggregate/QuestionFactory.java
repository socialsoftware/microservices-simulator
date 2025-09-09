package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class QuestionFactory {

    public Question createQuestion(Integer aggregateId, QuestionDto questionDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new Question(
            questionDto.getTitle(),
            questionDto.getContent(),
            questionDto.getNumberOfOptions(),
            questionDto.getCorrectOption(),
            questionDto.getOrder(),
            questionDto.getCourse(),
            questionDto.getTopics(),
            questionDto.getOptions()
        );
    }

    public Question createQuestionFromExisting(Question existingQuestion) {
        // Create a copy of the existing aggregate
        if (existingQuestion instanceof Question) {
            return new Question((Question) existingQuestion);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public QuestionDto createQuestionDto(Question question) {
        return new QuestionDto((Question) question);
    }
}
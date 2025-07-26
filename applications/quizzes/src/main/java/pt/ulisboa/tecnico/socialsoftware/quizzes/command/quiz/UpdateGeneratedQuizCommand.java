package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.Set;

public class UpdateGeneratedQuizCommand extends Command {
    private QuizDto quizDto;
    private Set<Integer> topicsAggregateIds;
    private Integer numberOfQuestions;

    public UpdateGeneratedQuizCommand(UnitOfWork unitOfWork, String serviceName, QuizDto quizDto,
            Set<Integer> topicsAggregateIds, Integer numberOfQuestions) {
        super(unitOfWork, serviceName, quizDto.getAggregateId());
        this.quizDto = quizDto;
        this.topicsAggregateIds = topicsAggregateIds;
        this.numberOfQuestions = numberOfQuestions;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public Set<Integer> getTopicsAggregateIds() {
        return topicsAggregateIds;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }
}

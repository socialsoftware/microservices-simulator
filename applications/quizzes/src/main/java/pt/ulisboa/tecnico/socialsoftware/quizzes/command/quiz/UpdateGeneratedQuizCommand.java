package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.List;
import java.util.Set;

public class UpdateGeneratedQuizCommand extends Command {
    private QuizDto quizDto;
    private Set<Integer> topicsAggregateIds;
    private Integer numberOfQuestions;
    private List<QuestionDto> questionDtos;

    public UpdateGeneratedQuizCommand(UnitOfWork unitOfWork, String serviceName, QuizDto quizDto,
            Set<Integer> topicsAggregateIds, Integer numberOfQuestions, List<QuestionDto> questionDtos) {
        super(unitOfWork, serviceName, quizDto.getAggregateId());
        this.quizDto = quizDto;
        this.topicsAggregateIds = topicsAggregateIds;
        this.numberOfQuestions = numberOfQuestions;
        this.questionDtos = questionDtos;
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

    public List<QuestionDto> getQuestionDtos() {
        return questionDtos;
    }
}

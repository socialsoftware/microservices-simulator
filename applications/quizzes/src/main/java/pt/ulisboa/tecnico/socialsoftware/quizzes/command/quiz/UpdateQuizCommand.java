package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;

import java.util.Set;

public class UpdateQuizCommand extends Command {
    private QuizDto quizDto;
    private Set<QuizQuestion> quizQuestions;

    public UpdateQuizCommand(UnitOfWork unitOfWork, String serviceName, QuizDto quizDto,
            Set<QuizQuestion> quizQuestions) {
        super(unitOfWork, serviceName, quizDto.getAggregateId());
        this.quizDto = quizDto;
        this.quizQuestions = quizQuestions;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public Set<QuizQuestion> getQuizQuestions() {
        return quizQuestions;
    }
}

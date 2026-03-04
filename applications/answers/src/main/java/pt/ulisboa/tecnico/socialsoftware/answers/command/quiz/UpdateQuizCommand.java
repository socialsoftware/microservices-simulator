package pt.ulisboa.tecnico.socialsoftware.answers.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

public class UpdateQuizCommand extends Command {
    private final QuizDto quizDto;

    public UpdateQuizCommand(UnitOfWork unitOfWork, String serviceName, QuizDto quizDto) {
        super(unitOfWork, serviceName, null);
        this.quizDto = quizDto;
    }

    public QuizDto getQuizDto() { return quizDto; }
}

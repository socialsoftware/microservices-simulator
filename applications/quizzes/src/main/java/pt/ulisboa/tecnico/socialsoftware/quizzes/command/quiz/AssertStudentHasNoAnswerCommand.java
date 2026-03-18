package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class AssertStudentHasNoAnswerCommand extends Command {
    private final Integer quizAggregateId;
    private final Integer studentAggregateId;

    public AssertStudentHasNoAnswerCommand(UnitOfWork unitOfWork, String serviceName,
            Integer quizAggregateId, Integer studentAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.studentAggregateId = studentAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }
}

package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class UpdateExecutionCommand extends Command {
    private final Integer executionAggregateId;
    private final String acronym;
    private final String academicTerm;

    public UpdateExecutionCommand(UnitOfWork unitOfWork, String serviceName,
                                  Integer executionAggregateId, String acronym, String academicTerm) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.acronym = acronym;
        this.academicTerm = academicTerm;
    }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public String getAcronym() { return acronym; }
    public String getAcademicTerm() { return academicTerm; }
}

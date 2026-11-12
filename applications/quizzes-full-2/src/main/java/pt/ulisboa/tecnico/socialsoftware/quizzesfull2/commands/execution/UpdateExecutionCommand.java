package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class UpdateExecutionCommand extends Command {
    private Integer executionAggregateId;
    private String acronym;
    private String academicTerm;

    protected UpdateExecutionCommand() {}

    public UpdateExecutionCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId,
                                  String acronym, String academicTerm) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.acronym = acronym;
        this.academicTerm = academicTerm;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getAcademicTerm() {
        return academicTerm;
    }

    public void setAcademicTerm(String academicTerm) {
        this.academicTerm = academicTerm;
    }
}

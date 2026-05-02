package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse;

public class CreateExecutionCommand extends Command {
    private final String acronym;
    private final String academicTerm;
    private final ExecutionCourse executionCourse;

    public CreateExecutionCommand(UnitOfWork unitOfWork, String serviceName,
                                  String acronym, String academicTerm, ExecutionCourse executionCourse) {
        super(unitOfWork, serviceName, null);
        this.acronym = acronym;
        this.academicTerm = academicTerm;
        this.executionCourse = executionCourse;
    }

    public String getAcronym() { return acronym; }
    public String getAcademicTerm() { return academicTerm; }
    public ExecutionCourse getExecutionCourse() { return executionCourse; }
}

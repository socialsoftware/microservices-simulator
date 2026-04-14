package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetEnrollmentTeacherCommand extends Command {
    private final Integer enrollmentId;
    private final Integer teacherAggregateId;

    public GetEnrollmentTeacherCommand(UnitOfWork unitOfWork, String serviceName, Integer enrollmentId, Integer teacherAggregateId) {
        super(unitOfWork, serviceName, null);
        this.enrollmentId = enrollmentId;
        this.teacherAggregateId = teacherAggregateId;
    }

    public Integer getEnrollmentId() { return enrollmentId; }
    public Integer getTeacherAggregateId() { return teacherAggregateId; }
}

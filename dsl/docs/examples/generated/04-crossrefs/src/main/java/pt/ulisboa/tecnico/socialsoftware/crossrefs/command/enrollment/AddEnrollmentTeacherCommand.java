package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;

public class AddEnrollmentTeacherCommand extends Command {
    private final Integer enrollmentId;
    private final Integer teacherAggregateId;
    private final EnrollmentTeacherDto teacherDto;

    public AddEnrollmentTeacherCommand(UnitOfWork unitOfWork, String serviceName, Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto) {
        super(unitOfWork, serviceName, null);
        this.enrollmentId = enrollmentId;
        this.teacherAggregateId = teacherAggregateId;
        this.teacherDto = teacherDto;
    }

    public Integer getEnrollmentId() { return enrollmentId; }
    public Integer getTeacherAggregateId() { return teacherAggregateId; }
    public EnrollmentTeacherDto getTeacherDto() { return teacherDto; }
}

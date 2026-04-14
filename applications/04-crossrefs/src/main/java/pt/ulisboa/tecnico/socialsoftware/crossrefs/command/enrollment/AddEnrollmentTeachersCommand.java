package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import java.util.List;

public class AddEnrollmentTeachersCommand extends Command {
    private final Integer enrollmentId;
    private final List<EnrollmentTeacherDto> teacherDtos;

    public AddEnrollmentTeachersCommand(UnitOfWork unitOfWork, String serviceName, Integer enrollmentId, List<EnrollmentTeacherDto> teacherDtos) {
        super(unitOfWork, serviceName, null);
        this.enrollmentId = enrollmentId;
        this.teacherDtos = teacherDtos;
    }

    public Integer getEnrollmentId() { return enrollmentId; }
    public List<EnrollmentTeacherDto> getTeacherDtos() { return teacherDtos; }
}

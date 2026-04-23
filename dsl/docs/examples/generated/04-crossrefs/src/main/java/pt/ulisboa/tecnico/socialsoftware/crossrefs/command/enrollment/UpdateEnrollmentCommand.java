package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;

public class UpdateEnrollmentCommand extends Command {
    private final EnrollmentDto enrollmentDto;

    public UpdateEnrollmentCommand(UnitOfWork unitOfWork, String serviceName, EnrollmentDto enrollmentDto) {
        super(unitOfWork, serviceName, null);
        this.enrollmentDto = enrollmentDto;
    }

    public EnrollmentDto getEnrollmentDto() { return enrollmentDto; }
}

package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.teacher;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

public class UpdateTeacherCommand extends Command {
    private final TeacherDto teacherDto;

    public UpdateTeacherCommand(UnitOfWork unitOfWork, String serviceName, TeacherDto teacherDto) {
        super(unitOfWork, serviceName, null);
        this.teacherDto = teacherDto;
    }

    public TeacherDto getTeacherDto() { return teacherDto; }
}

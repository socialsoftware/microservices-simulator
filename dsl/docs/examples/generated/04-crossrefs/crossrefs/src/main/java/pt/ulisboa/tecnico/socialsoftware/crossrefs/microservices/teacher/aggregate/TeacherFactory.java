package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

public interface TeacherFactory {
    Teacher createTeacher(Integer aggregateId, TeacherDto teacherDto);
    Teacher createTeacherFromExisting(Teacher existingTeacher);
    TeacherDto createTeacherDto(Teacher teacher);
}

package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.TeacherFactory;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.SagaTeacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.dtos.SagaTeacherDto;

@Service
@Profile("sagas")
public class SagasTeacherFactory implements TeacherFactory {
    @Override
    public Teacher createTeacher(Integer aggregateId, TeacherDto teacherDto) {
        return new SagaTeacher(aggregateId, teacherDto);
    }

    @Override
    public Teacher createTeacherFromExisting(Teacher existingTeacher) {
        return new SagaTeacher((SagaTeacher) existingTeacher);
    }

    @Override
    public TeacherDto createTeacherDto(Teacher teacher) {
        return new SagaTeacherDto(teacher);
    }
}
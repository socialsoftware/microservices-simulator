package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.SagaTeacher;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaTeacherDto extends TeacherDto {
private SagaState sagaState;

public SagaTeacherDto(Teacher teacher) {
super((Teacher) teacher);
this.sagaState = ((SagaTeacher)teacher).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}
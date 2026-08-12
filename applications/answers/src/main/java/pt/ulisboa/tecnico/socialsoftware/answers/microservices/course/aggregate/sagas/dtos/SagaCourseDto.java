package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.sagas.SagaCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaCourseDto extends CourseDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaCourseDto(Course course) {
super((Course) course);
this.sagaState = ((SagaCourse)course).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}
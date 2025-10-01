package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaCourse;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaCourseDto extends CourseDto {
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
package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.CourseDto;

@Entity
public class SagaCourse extends Course implements SagaAggregate {
private SagaState sagaState;

public SagaCourse() {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
}

public SagaCourse(SagaCourse other) {
super();
this.sagaState = other.getSagaState();
}

public SagaCourse(CourseDto courseDto) {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
// TODO: Initialize from DTO properties
}

@Override
public void setSagaState(SagaState state) {
this.sagaState = state;
}

@Override
public SagaState getSagaState() {
return this.sagaState;
}
}
package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.SagaEnrollment;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaEnrollmentDto extends EnrollmentDto {
private SagaState sagaState;

public SagaEnrollmentDto(Enrollment enrollment) {
super((Enrollment) enrollment);
this.sagaState = ((SagaEnrollment)enrollment).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}
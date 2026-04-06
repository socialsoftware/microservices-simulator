package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;

@Entity
public class SagaEnrollment extends Enrollment implements SagaAggregate {
    private SagaState sagaState;

    public SagaEnrollment() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaEnrollment(SagaEnrollment other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaEnrollment(Integer aggregateId, EnrollmentDto enrollmentDto) {
        super(aggregateId, enrollmentDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
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
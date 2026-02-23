package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

@Entity
public class SagaTeacher extends Teacher implements SagaAggregate {
    private SagaState sagaState;

    public SagaTeacher() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTeacher(SagaTeacher other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaTeacher(Integer aggregateId, TeacherDto teacherDto) {
        super(aggregateId, teacherDto);
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
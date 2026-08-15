package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.Execution;

import java.time.LocalDateTime;

@Entity
public class SagaExecution extends Execution implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaExecution() {
    }

    public SagaExecution(Integer aggregateId, Integer courseAggregateId, String courseName, CourseType courseType,
                         String acronym, String academicTerm, LocalDateTime endDate) {
        super(aggregateId, courseAggregateId, courseName, courseType, acronym, academicTerm, endDate);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaExecution(SagaExecution other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;

@Entity
public class SagaCourse extends Course implements SagaAggregate {
    @OneToOne
    private SagaState sagaState;
    
    public SagaCourse() {
        super();
    }

    public SagaCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        super(aggregateId, courseExecutionDto);
    }

    public SagaCourse(SagaCourse other) {
        super(other);
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

package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaCourseExecutionDto extends CourseExecutionDto {
    private SagaState sagaState;

    public SagaCourseExecutionDto(CourseExecution courseExecution) {
        super(courseExecution);
        this.sagaState = ((SagaCourseExecution)courseExecution).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaCourseDto extends CourseDto {
    private SagaState sagaState;

    public SagaCourseDto(Course course) {
        super(course);
        this.sagaState = ((SagaCourse)course).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}

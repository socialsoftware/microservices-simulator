package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;

@Service
@Profile("sagas")
public class SagasCourseFactory implements CourseFactory {
    @Override
    public Course createCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        return new SagaCourse(aggregateId, courseExecutionDto);
    }
}

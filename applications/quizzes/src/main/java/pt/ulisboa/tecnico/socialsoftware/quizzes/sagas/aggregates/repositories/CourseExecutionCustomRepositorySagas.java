package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;

import java.util.Set;

@Service
@Profile("sagas")
public class CourseExecutionCustomRepositorySagas implements CourseExecutionCustomRepository {

    @Autowired
    private CourseExecutionRepository courseExecutionRepository;

    @Override
    public Set<Integer> findCourseExecutionIdsOfAllNonDeleted() {
        return courseExecutionRepository.findCourseExecutionIdsOfAllNonDeletedForSaga();
    }
}
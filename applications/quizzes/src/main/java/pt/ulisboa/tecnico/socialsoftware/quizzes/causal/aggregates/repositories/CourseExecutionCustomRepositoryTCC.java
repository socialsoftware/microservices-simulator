package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.repositories;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;

@Service
@Profile("tcc")
public class CourseExecutionCustomRepositoryTCC implements CourseExecutionCustomRepository {

    @Autowired
    private CourseExecutionRepository courseExecutionRepository;

    @Override
    public Set<Integer> findCourseExecutionIdsOfAllNonDeleted() {
        return courseExecutionRepository.findCourseExecutionIdsOfAllNonDeletedForTCC();
    }
}
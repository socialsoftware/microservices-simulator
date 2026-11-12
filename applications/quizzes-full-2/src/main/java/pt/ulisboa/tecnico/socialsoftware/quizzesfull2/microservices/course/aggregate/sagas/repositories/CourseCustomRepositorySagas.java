package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class CourseCustomRepositorySagas implements CourseCustomRepository {
    @Autowired
    private CourseRepository courseRepository;

    @Override
    public Set<Integer> findAllCourseIds() {
        return courseRepository.findAllLatestActive().stream()
                .map(Aggregate::getAggregateId)
                .collect(Collectors.toSet());
    }
}

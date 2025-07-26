package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseRepository;

import java.util.Optional;

@Profile("tcc")
@Service
public class CourseCustomRepositoryTCC implements CourseCustomRepository {

    @Autowired
    private CourseRepository courseRepository;

    @Override
    public Optional<Integer> findCourseIdByName(String courseName) {
        return courseRepository.findCourseIdByNameForTCC(courseName);
    }
}
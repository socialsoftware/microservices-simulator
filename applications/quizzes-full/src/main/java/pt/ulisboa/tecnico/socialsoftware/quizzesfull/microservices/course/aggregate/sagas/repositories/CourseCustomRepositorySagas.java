package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseRepository;

@Service
@Profile("sagas")
public class CourseCustomRepositorySagas implements CourseCustomRepository {

    @Autowired
    private CourseRepository courseRepository;
}

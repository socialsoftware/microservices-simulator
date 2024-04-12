package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseFunctionalitiesInterface;

@Profile("sagas")
@Service
public class SagaCourseFunctionalities implements CourseFunctionalitiesInterface {

}

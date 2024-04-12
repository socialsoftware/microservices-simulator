package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseFunctionalitiesInterface;

@Profile("tcc")
@Service
public class CausalCourseFunctionalities implements CourseFunctionalitiesInterface {

}

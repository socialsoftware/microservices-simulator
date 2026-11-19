package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.coordination.functionalities.CourseFunctionalities;

@RestController
public class CourseController {

    @Autowired
    private CourseFunctionalities courseFunctionalities;

    @GetMapping("/courses/{courseAggregateId}")
    public CourseDto getCourseById(@PathVariable Integer courseAggregateId) {
        return courseFunctionalities.getCourseById(courseAggregateId);
    }
}

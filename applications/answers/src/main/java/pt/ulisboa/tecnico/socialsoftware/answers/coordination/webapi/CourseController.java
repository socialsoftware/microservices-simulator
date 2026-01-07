package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.CourseFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;

@RestController
public class CourseController {
    @Autowired
    private CourseFunctionalities courseFunctionalities;

    @PostMapping("/courses/create")
    public CourseDto createCourse(@RequestBody CourseDto courseDto) {
        return courseFunctionalities.createCourse(courseDto);
    }

    @GetMapping("/courses/{courseAggregateId}")
    public CourseDto getCourseById(@PathVariable Integer courseAggregateId) {
        return courseFunctionalities.getCourseById(courseAggregateId);
    }

    @PutMapping("/courses")
    public CourseDto updateCourse(@RequestBody CourseDto courseDto) {
        return courseFunctionalities.updateCourse(courseDto);
    }

    @DeleteMapping("/courses/{courseAggregateId}")
    public void deleteCourse(@PathVariable Integer courseAggregateId) {
        courseFunctionalities.deleteCourse(courseAggregateId);
    }

    @GetMapping("/courses")
    public List<CourseDto> searchCourses(@RequestParam(required = false) String name, @RequestParam(required = false) CourseType type) {
        return courseFunctionalities.searchCourses(name, type);
    }
}

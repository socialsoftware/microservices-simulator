package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
public class CourseController {

@Autowired
private CourseService courseService;

@GetMapping(value = "/courses")
public ResponseEntity<List<CourseDto>> getAllCourses(
        ) {
        List<CourseDto> result = courseService.getAllCourses();
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/courses/{id}")
public ResponseEntity<CourseDto> getCourse(
        @PathVariable Long id
        ) throws Exception {
        CourseDto result = courseService.getCourse(id);
        return ResponseEntity.ok(result);
        }

@PostMapping(value = "/courses")
public ResponseEntity<CourseDto> createCourse(
        @RequestBody CourseDto courseDto
        ) throws Exception {
        CourseDto result = courseService.createCourse(courseDto);
        return ResponseEntity.ok(result);
        }

@PutMapping(value = "/courses/{id}")
public ResponseEntity<CourseDto> updateCourse(
        @PathVariable Long id,
        @RequestBody CourseDto courseDto
        ) throws Exception {
        CourseDto result = courseService.updateCourse(id,
        courseDto);
        return ResponseEntity.ok(result);
        }

@DeleteMapping(value = "/courses/{id}")
public ResponseEntity<Void> deleteCourse(
        @PathVariable Long id
        ) throws Exception {
        courseService.deleteCourse(id);
        return ResponseEntity.ok().build();
        }

        }
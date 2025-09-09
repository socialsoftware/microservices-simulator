package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/courseexecution")
public class CourseExecutionController {

@Autowired
private CourseExecutionService courseexecutionService;

@GetMapping(value = "/courseexecutions")
public ResponseEntity<List<CourseExecutionDto>> getAllCourseExecutions(
        ) {
        List<CourseExecutionDto> result = courseexecutionService.getAllCourseExecutions();
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/courseexecutions/{id}")
public ResponseEntity<CourseExecutionDto> getCourseExecution(
        @PathVariable Long id
        ) throws Exception {
        CourseExecutionDto result = courseexecutionService.getCourseExecution(id);
        return ResponseEntity.ok(result);
        }

@PostMapping(value = "/courseexecutions")
public ResponseEntity<CourseExecutionDto> createCourseExecution(
        @RequestBody CourseExecutionDto courseexecutionDto
        ) throws Exception {
        CourseExecutionDto result = courseexecutionService.createCourseExecution(courseexecutionDto);
        return ResponseEntity.ok(result);
        }

@PutMapping(value = "/courseexecutions/{id}")
public ResponseEntity<CourseExecutionDto> updateCourseExecution(
        @PathVariable Long id,
        @RequestBody CourseExecutionDto courseexecutionDto
        ) throws Exception {
        CourseExecutionDto result = courseexecutionService.updateCourseExecution(id,
        courseexecutionDto);
        return ResponseEntity.ok(result);
        }

@DeleteMapping(value = "/courseexecutions/{id}")
public ResponseEntity<Void> deleteCourseExecution(
        @PathVariable Long id
        ) throws Exception {
        courseexecutionService.deleteCourseExecution(id);
        return ResponseEntity.ok().build();
        }

        }
package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.functionalities.EnrollmentFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.webapi.requestDtos.CreateEnrollmentRequestDto;

@RestController
public class EnrollmentController {
    @Autowired
    private EnrollmentFunctionalities enrollmentFunctionalities;

    @PostMapping("/enrollments/create")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentDto createEnrollment(@RequestBody CreateEnrollmentRequestDto createRequest) {
        return enrollmentFunctionalities.createEnrollment(createRequest);
    }

    @GetMapping("/enrollments/{enrollmentAggregateId}")
    public EnrollmentDto getEnrollmentById(@PathVariable Integer enrollmentAggregateId) {
        return enrollmentFunctionalities.getEnrollmentById(enrollmentAggregateId);
    }

    @PutMapping("/enrollments")
    public EnrollmentDto updateEnrollment(@RequestBody EnrollmentDto enrollmentDto) {
        return enrollmentFunctionalities.updateEnrollment(enrollmentDto);
    }

    @DeleteMapping("/enrollments/{enrollmentAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEnrollment(@PathVariable Integer enrollmentAggregateId) {
        enrollmentFunctionalities.deleteEnrollment(enrollmentAggregateId);
    }

    @GetMapping("/enrollments")
    public List<EnrollmentDto> getAllEnrollments() {
        return enrollmentFunctionalities.getAllEnrollments();
    }

    @PostMapping("/enrollments/{enrollmentId}/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentTeacherDto addEnrollmentTeacher(@PathVariable Integer enrollmentId, @RequestParam Integer teacherAggregateId, @RequestBody EnrollmentTeacherDto teacherDto) {
        return enrollmentFunctionalities.addEnrollmentTeacher(enrollmentId, teacherAggregateId, teacherDto);
    }

    @PostMapping("/enrollments/{enrollmentId}/teachers/batch")
    public List<EnrollmentTeacherDto> addEnrollmentTeachers(@PathVariable Integer enrollmentId, @RequestBody List<EnrollmentTeacherDto> teacherDtos) {
        return enrollmentFunctionalities.addEnrollmentTeachers(enrollmentId, teacherDtos);
    }

    @GetMapping("/enrollments/{enrollmentId}/teachers/{teacherAggregateId}")
    public EnrollmentTeacherDto getEnrollmentTeacher(@PathVariable Integer enrollmentId, @PathVariable Integer teacherAggregateId) {
        return enrollmentFunctionalities.getEnrollmentTeacher(enrollmentId, teacherAggregateId);
    }

    @PutMapping("/enrollments/{enrollmentId}/teachers/{teacherAggregateId}")
    public EnrollmentTeacherDto updateEnrollmentTeacher(@PathVariable Integer enrollmentId, @PathVariable Integer teacherAggregateId, @RequestBody EnrollmentTeacherDto teacherDto) {
        return enrollmentFunctionalities.updateEnrollmentTeacher(enrollmentId, teacherAggregateId, teacherDto);
    }

    @DeleteMapping("/enrollments/{enrollmentId}/teachers/{teacherAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEnrollmentTeacher(@PathVariable Integer enrollmentId, @PathVariable Integer teacherAggregateId) {
        enrollmentFunctionalities.removeEnrollmentTeacher(enrollmentId, teacherAggregateId);
    }
}

package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.functionalities.TeacherFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.webapi.requestDtos.CreateTeacherRequestDto;

@RestController
public class TeacherController {
    @Autowired
    private TeacherFunctionalities teacherFunctionalities;

    @PostMapping("/teachers/create")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherDto createTeacher(@RequestBody CreateTeacherRequestDto createRequest) {
        return teacherFunctionalities.createTeacher(createRequest);
    }

    @GetMapping("/teachers/{teacherAggregateId}")
    public TeacherDto getTeacherById(@PathVariable Integer teacherAggregateId) {
        return teacherFunctionalities.getTeacherById(teacherAggregateId);
    }

    @PutMapping("/teachers")
    public TeacherDto updateTeacher(@RequestBody TeacherDto teacherDto) {
        return teacherFunctionalities.updateTeacher(teacherDto);
    }

    @DeleteMapping("/teachers/{teacherAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacher(@PathVariable Integer teacherAggregateId) {
        teacherFunctionalities.deleteTeacher(teacherAggregateId);
    }

    @GetMapping("/teachers")
    public List<TeacherDto> getAllTeachers() {
        return teacherFunctionalities.getAllTeachers();
    }
}

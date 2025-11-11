package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserFunctionalities userFunctionalities;

    @PostMapping("/users/create")
    public UserDto createUser(@RequestBody UserDto userDto) throws Exception {
        UserDto result = userFunctionalities.createUser(userDto);
        return result;
    }

    @GetMapping("/users/{userAggregateId}")
    public UserDto findByUserId(@PathVariable Integer userAggregateId) {
        UserDto result = userFunctionalities.findByUserId(userAggregateId);
        return result;
    }

    @PostMapping("/users/{userAggregateId}/activate")
    public void activateUser(@PathVariable Integer userAggregateId) throws Exception {
        userFunctionalities.activateUser(userAggregateId);
    }

    @PostMapping("/users/{userAggregateId}/delete")
    public void deleteUser(@PathVariable Integer userAggregateId) throws Exception {
        userFunctionalities.deleteUser(userAggregateId);
    }

    @GetMapping("/users/students")
    public List<UserDto> getStudents() {
        List<UserDto> result = userFunctionalities.getStudents();
        return result;
    }

    @GetMapping("/users/teachers")
    public List<UserDto> getTeachers() {
        List<UserDto> result = userFunctionalities.getTeachers();
        return result;
    }
}

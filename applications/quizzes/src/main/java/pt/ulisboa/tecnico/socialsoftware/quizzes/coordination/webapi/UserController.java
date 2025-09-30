package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserFunctionalities userFunctionalities;

    @PostMapping("/users/create")
    public UserDto createUser(@RequestBody UserDto userDto) throws Exception {
        return userFunctionalities.createUser(userDto);
    }

    @GetMapping("/users/{userAggregateId}")
    public UserDto findByAggregateId(@PathVariable Integer userAggregateId) {
        return userFunctionalities.findByUserId(userAggregateId);
    }

    @PostMapping("/users/{userAggregateId}/activate")
    public void activateUser(@PathVariable Integer userAggregateId) throws Exception {
        userFunctionalities.activateUser(userAggregateId);
    }

    @PostMapping("/users/{userAggregateId}/deactivate")
    public void deactivateUser(@PathVariable Integer userAggregateId) throws Exception {
        userFunctionalities.deactivateUser(userAggregateId);
    }

    @PostMapping("/users/{userAggregateId}/delete")
    public void deleteUser(@PathVariable Integer userAggregateId) throws Exception {
        userFunctionalities.deleteUser(userAggregateId);
    }

    @GetMapping("/users/students")
    public List<UserDto> getStudents() {
        return userFunctionalities.getStudents();
    }

    @GetMapping("/users/teachers")
    public List<UserDto> getTeachers() {
        return userFunctionalities.getTeachers();
    }
}

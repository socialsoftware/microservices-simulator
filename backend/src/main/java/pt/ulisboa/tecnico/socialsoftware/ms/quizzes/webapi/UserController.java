package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.webapi;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserFunctionalitiesInterface;

@RestController
public class UserController {
    @Autowired
    private UserFunctionalitiesInterface userFunctionalities;

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

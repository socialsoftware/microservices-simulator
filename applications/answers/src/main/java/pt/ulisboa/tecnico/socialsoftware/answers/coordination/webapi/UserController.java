package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.UserFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateUserRequestDto;

@RestController
public class UserController {
    @Autowired
    private UserFunctionalities userFunctionalities;

    @PostMapping("/users/create")
    public UserDto createUser(@RequestBody CreateUserRequestDto createRequest) {
        return userFunctionalities.createUser(createRequest);
    }

    @GetMapping("/users/{userAggregateId}")
    public UserDto getUserById(@PathVariable Integer userAggregateId) {
        return userFunctionalities.getUserById(userAggregateId);
    }

    @PutMapping("/users")
    public UserDto updateUser(@RequestBody UserDto userDto) {
        return userFunctionalities.updateUser(userDto);
    }

    @DeleteMapping("/users/{userAggregateId}")
    public void deleteUser(@PathVariable Integer userAggregateId) {
        userFunctionalities.deleteUser(userAggregateId);
    }

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        return userFunctionalities.getAllUsers();
    }
}

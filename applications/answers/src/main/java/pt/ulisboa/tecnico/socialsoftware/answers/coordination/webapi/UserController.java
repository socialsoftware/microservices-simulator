package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.UserFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

@RestController
public class UserController {
    @Autowired
    private UserFunctionalities userFunctionalities;

    @PostMapping("/users/create")
    public UserDto createUser(@RequestBody UserDto userDto) {
        return userFunctionalities.createUser(userDto);
    }

    @GetMapping("/users/{userAggregateId}")
    public UserDto getUserById(@PathVariable Integer userAggregateId) {
        return userFunctionalities.getUserById(userAggregateId);
    }

    @PutMapping("/users/{userAggregateId}")
    public UserDto updateUser(@PathVariable Integer userAggregateId, @RequestBody UserDto userDto) {
        return userFunctionalities.updateUser(userAggregateId, userDto);
    }

    @DeleteMapping("/users/{userAggregateId}")
    public void deleteUser(@PathVariable Integer userAggregateId) {
        userFunctionalities.deleteUser(userAggregateId);
    }

    @GetMapping("/users")
    public List<UserDto> searchUsers(@RequestParam(required = false) String name, @RequestParam(required = false) String username, @RequestParam(required = false) UserRole role, @RequestParam(required = false) Boolean active) {
        return userFunctionalities.searchUsers(name, username, role, active);
    }
}

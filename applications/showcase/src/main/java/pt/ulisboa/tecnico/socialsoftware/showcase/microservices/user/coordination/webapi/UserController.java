package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.functionalities.UserFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;

@RestController
public class UserController {
    @Autowired
    private UserFunctionalities userFunctionalities;

    @PostMapping("/users/create")
    @ResponseStatus(HttpStatus.CREATED)
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Integer userAggregateId) {
        userFunctionalities.deleteUser(userAggregateId);
    }

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        return userFunctionalities.getAllUsers();
    }

    @PostMapping("/users/signup")
    public void signUp(@RequestParam String username, @RequestParam String email) {
        userFunctionalities.signUp(username, email);
    }

    @PostMapping("/users/{userId}/loyalty")
    public void awardLoyaltyPoints(@PathVariable Integer userId, @RequestParam Integer points) {
        userFunctionalities.awardLoyaltyPoints(userId, points);
    }
}

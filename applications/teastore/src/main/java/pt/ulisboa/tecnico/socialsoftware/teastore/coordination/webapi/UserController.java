package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.*;

@RestController
public class UserController {
    @Autowired
    private UserFunctionalities userFunctionalities;

    @PostMapping("/users/create")
    public UserDto createUser(@RequestBody UserDto userDto) throws Exception {
        return userFunctionalities.createUser(userDto);
    }

    @GetMapping("/users/{userAggregateId}")
    public UserDto findByUserId(@PathVariable Integer userAggregateId) {
        return userFunctionalities.findByUserId(userAggregateId);
    }

    @DeleteMapping("/users/{userAggregateId}/delete")
    public void deleteUser(@PathVariable Integer userAggregateId) throws Exception {
        userFunctionalities.deleteUser(userAggregateId);
    }
}

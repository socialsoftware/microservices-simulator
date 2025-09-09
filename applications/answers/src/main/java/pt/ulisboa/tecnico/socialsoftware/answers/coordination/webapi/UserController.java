package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

@Autowired
private UserService userService;

@GetMapping(value = "/users")
public ResponseEntity<List<UserDto>> getAllUsers(
        ) {
        List<UserDto> result = userService.getAllUsers();
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/users/{id}")
public ResponseEntity<UserDto> getUser(
        @PathVariable Long id
        ) throws Exception {
        UserDto result = userService.getUser(id);
        return ResponseEntity.ok(result);
        }

@PostMapping(value = "/users")
public ResponseEntity<UserDto> createUser(
        @RequestBody UserDto userDto
        ) throws Exception {
        UserDto result = userService.createUser(userDto);
        return ResponseEntity.ok(result);
        }

@PutMapping(value = "/users/{id}")
public ResponseEntity<UserDto> updateUser(
        @PathVariable Long id,
        @RequestBody UserDto userDto
        ) throws Exception {
        UserDto result = userService.updateUser(id,
        userDto);
        return ResponseEntity.ok(result);
        }

@DeleteMapping(value = "/users/{id}")
public ResponseEntity<Void> deleteUser(
        @PathVariable Long id
        ) throws Exception {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
        }

        }
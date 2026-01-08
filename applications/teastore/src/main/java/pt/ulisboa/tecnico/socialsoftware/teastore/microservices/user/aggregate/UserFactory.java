package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto userDto);
    User createUserFromExisting(User existingUser);
    UserDto createUserDto(User user);
}

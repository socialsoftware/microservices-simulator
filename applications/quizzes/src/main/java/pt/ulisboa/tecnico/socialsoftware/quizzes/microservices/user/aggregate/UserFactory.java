package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto userDto);
    User createUserFromExisting(User existingUser);
    UserDto createUserDto(User user);
}

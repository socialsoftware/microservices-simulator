package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto userDto);

    User createUserFromExisting(User existing);

    UserDto createUserDto(User user);
}

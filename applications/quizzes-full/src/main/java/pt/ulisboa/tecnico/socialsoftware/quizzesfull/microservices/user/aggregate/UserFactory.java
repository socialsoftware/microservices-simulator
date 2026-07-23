package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto userDto);
    User createUserCopy(User existing);
    UserDto createUserDto(User user);
}

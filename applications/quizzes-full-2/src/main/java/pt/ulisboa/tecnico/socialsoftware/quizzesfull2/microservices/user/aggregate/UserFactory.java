package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate;

public interface UserFactory {
    User createUser(Integer aggregateId, String name, String username, Role role);

    User createUserCopy(User existing);

    UserDto createUserDto(User user);
}

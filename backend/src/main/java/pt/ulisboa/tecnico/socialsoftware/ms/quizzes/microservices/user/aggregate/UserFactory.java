package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto userDto);
    User createUserFromExisting(User existingUser);
}

package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

public interface UserFactory {
    User createUser(Integer aggregateId,  Dto);
    User createUserFromExisting(User existingUser);
     createUserDto(User );
}

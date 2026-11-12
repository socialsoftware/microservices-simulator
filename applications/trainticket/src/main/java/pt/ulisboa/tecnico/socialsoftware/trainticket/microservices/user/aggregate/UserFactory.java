package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto userDto);

    User createUserCopy(User existing);

    UserDto createUserDto(User user);
}

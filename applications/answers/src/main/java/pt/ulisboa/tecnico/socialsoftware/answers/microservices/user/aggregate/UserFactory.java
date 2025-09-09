package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class UserFactory {

    public User createUser(Integer aggregateId, UserDto userDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new User(
            userDto.getName(),
            userDto.getUsername(),
            userDto.getActive()
        );
    }

    public User createUserFromExisting(User existingUser) {
        // Create a copy of the existing aggregate
        if (existingUser instanceof User) {
            return new User((User) existingUser);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public UserDto createUserDto(User user) {
        return new UserDto((User) user);
    }
}
package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

public interface UserFunctionalitiesInterface {
    UserDto createUser(UserDto userDto) throws Exception;
    UserDto findByUserId(Integer userAggregateId);
    void activateUser(Integer userAggregateId) throws Exception;
    void deleteUser(Integer userAggregateId) throws Exception;
    List<UserDto> getStudents();
    List<UserDto> getTeachers();
}
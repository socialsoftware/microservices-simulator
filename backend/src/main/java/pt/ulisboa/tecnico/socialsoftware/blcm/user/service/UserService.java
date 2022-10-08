package pt.ulisboa.tecnico.socialsoftware.blcm.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.service.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.AnonymizeUserEvent;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.DomainEvent;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.utils.ProcessedEvents;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.utils.ProcessedEventsRepository;
import pt.ulisboa.tecnico.socialsoftware.blcm.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.blcm.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.blcm.execution.dto.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.unityOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.repository.UserRepository;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.domain.Role;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.domain.User;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.domain.UserCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.dto.UserDto;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate.AggregateState.DELETED;
import static pt.ulisboa.tecnico.socialsoftware.blcm.exception.ErrorMessage.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ProcessedEventsRepository processedEventsRepository;

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserDto getCausalUserRemote(Integer aggregateId, UnitOfWork unitOfWork) {
        return new UserDto(getCausalUserLocal(aggregateId, unitOfWork));
    }

    // intended for requests from local functionalities
    public User getCausalUserLocal(Integer aggregateId, UnitOfWork unitOfWork) {
        User user = userRepository.findCausal(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(USER_NOT_FOUND, aggregateId));

        if(user.getState() == DELETED) {
            throw new TutorException(ErrorMessage.USER_DELETED, user.getAggregateId());
        }

        Set<DomainEvent> allEvents = new HashSet<>(eventRepository.findAll());
        Set<ProcessedEvents> processedEvents = new HashSet<>(processedEventsRepository.findAll());

        unitOfWork.addToCausalSnapshot(user, allEvents, processedEvents);
        return user;
    }

    /*simple user creation*/
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = new User(aggregateId, userDto);
        unitOfWork.addAggregateToCommit(user);
        return new UserDto(user);
    }


    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void anonymizeCourseExecutionUsers(Integer executionAggregateId, UnitOfWork unitOfWork) {
        Set<User> executionsUsers = userRepository.findCausalByExecution(executionAggregateId, unitOfWork.getVersion());
        executionsUsers.forEach(oldUser -> {
            User newUser = new User(oldUser);
            newUser.anonymize();
            unitOfWork.addAggregateToCommit(newUser);
            unitOfWork.addEvent(new AnonymizeUserEvent(newUser.getAggregateId(), "ANONYMOUS", "ANONYMOUS"));
        });
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void addCourseExecution(Integer userAggregateId, UserCourseExecution userCourseExecution, UnitOfWork unitOfWork) {
        User oldUser = getCausalUserLocal(userAggregateId, unitOfWork);

        if(!oldUser.isActive()){
            throw new TutorException(ErrorMessage.INACTIVE_USER);
        }

        User newUser = new User(oldUser);
        newUser.addCourseExecution(userCourseExecution);
        unitOfWork.addAggregateToCommit(newUser);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void activateUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = getCausalUserLocal(userAggregateId, unitOfWork);
        if(oldUser.isActive()) {
            throw new TutorException(USER_ACTIVE);
        }
        User newUser = new User(oldUser);
        newUser.setActive(true);
        unitOfWork.addAggregateToCommit(newUser);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Set<CourseExecutionDto> getUserCourseExecutions(Integer userAggregateId, UnitOfWork unitOfWork) {
        User user = getCausalUserLocal(userAggregateId, unitOfWork);
        return user.getCourseExecutions().stream()
                .map(UserCourseExecution::buildDto)
                .collect(Collectors.toSet());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = getCausalUserLocal(userAggregateId, unitOfWork);
        User newUser = new User(oldUser);
        newUser.remove();
        unitOfWork.addAggregateToCommit(newUser);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeCourseExecutionsFromUser(Integer userAggregateId, List<Integer> courseExecutionsAggregateIds, UnitOfWork unitOfWork) {
        User oldUser = getCausalUserLocal(userAggregateId, unitOfWork);
        Set<UserCourseExecution> courseExecutionsToRemove = oldUser.getCourseExecutions().stream()
                .filter(uce -> courseExecutionsAggregateIds.contains(uce.getAggregateId()))
                .collect(Collectors.toSet());

        if(!courseExecutionsToRemove.isEmpty()) {
            User newUser = new User(oldUser);
            for (UserCourseExecution userCourseExecution : courseExecutionsToRemove) {
                newUser.removeCourseExecution(userCourseExecution);
            }

            unitOfWork.addAggregateToCommit(newUser);
        }

    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<UserDto> getStudents(UnitOfWork unitOfWork) {
        Set<Integer> studentsIds = userRepository.findAll().stream()
                .filter(u -> u.getRole().equals(Role.STUDENT))
                .map(User::getAggregateId)
                .collect(Collectors.toSet());
        return studentsIds.stream()
                .map(id -> getCausalUserLocal(id, unitOfWork))
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<UserDto> getTeachers(UnitOfWork unitOfWork) {
        Set<Integer> teacherIds = userRepository.findAll().stream()
                .filter(u -> u.getRole().equals(Role.TEACHER))
                .map(User::getAggregateId)
                .collect(Collectors.toSet());
        return teacherIds.stream()
                .map(id -> getCausalUserLocal(id, unitOfWork))
                .map(UserDto::new)
                .collect(Collectors.toList());
    }
}

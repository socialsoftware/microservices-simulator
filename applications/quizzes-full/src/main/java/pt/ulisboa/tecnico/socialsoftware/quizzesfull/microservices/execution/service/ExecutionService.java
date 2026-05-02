package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.INACTIVE_USER;
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.NO_DUPLICATE_COURSE_EXECUTION;

@Service
public class ExecutionService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private ExecutionFactory executionFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final ExecutionCustomRepository executionRepository;

    public ExecutionService(UnitOfWorkService unitOfWorkService, ExecutionCustomRepository executionRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.executionRepository = executionRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto getExecutionById(Integer executionAggregateId, UnitOfWork unitOfWork) {
        return executionFactory.createExecutionDto(
                (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto createExecution(String acronym, String academicTerm, ExecutionCourse executionCourse,
                                        UnitOfWork unitOfWork) {
        // [P3] NO_DUPLICATE_COURSE_EXECUTION
        for (Integer id : executionRepository.findExecutionIdsOfAllNonDeleted()) {
            Execution existing = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (existing.getAcronym().equals(acronym) && existing.getAcademicTerm().equals(academicTerm)) {
                throw new QuizzesFullException(NO_DUPLICATE_COURSE_EXECUTION, acronym, academicTerm);
            }
        }
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Execution execution = executionFactory.createExecution(aggregateId, acronym, academicTerm, executionCourse);
        unitOfWorkService.registerChanged(execution, unitOfWork);
        return executionFactory.createExecutionDto(execution);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateExecution(Integer executionAggregateId, String acronym, String academicTerm,
                                UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);
        newExecution.setAcronym(acronym);
        newExecution.setAcademicTerm(academicTerm);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        // Copy-on-write: avoids JPA auto-flush of managed entity before abort queries
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);
        newExecution.remove();
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteCourseExecutionEvent(executionAggregateId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void enrollStudentInExecution(Integer executionAggregateId, UserDto userDto, UnitOfWork unitOfWork) {
        // [P3] INACTIVE_USER guard
        if (!userDto.isActive()) {
            throw new QuizzesFullException(INACTIVE_USER, userDto.getAggregateId());
        }
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);
        newExecution.addStudent(new ExecutionStudent(userDto));
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void disenrollStudent(Integer executionAggregateId, Integer userId, UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);
        newExecution.getStudents().stream()
                .filter(s -> s.getUserAggregateId().equals(userId))
                .findFirst()
                .ifPresent(newExecution::removeStudent);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(
                new DisenrollStudentFromCourseExecutionEvent(executionAggregateId, userId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateStudentNameInExecution(Integer executionAggregateId, Integer userId, String name,
                                             UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);
        newExecution.getStudents().stream()
                .filter(s -> s.getUserAggregateId().equals(userId))
                .findFirst()
                .ifPresent(s -> s.setUserName(name));
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void anonymizeStudentInExecution(Integer executionAggregateId, Integer userId, UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);
        newExecution.getStudents().stream()
                .filter(s -> s.getUserAggregateId().equals(userId))
                .findFirst()
                .ifPresent(s -> {
                    s.setUserName("ANONYMOUS");
                    s.setUserUsername("ANONYMOUS");
                    s.setActive(false);
                });
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionStudentDto getStudentByExecutionIdAndUserId(Integer executionAggregateId, Integer userId,
                                                                UnitOfWork unitOfWork) {
        Execution execution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        return execution.getStudents().stream()
                .filter(s -> s.getUserAggregateId().equals(userId))
                .findFirst()
                .map(ExecutionStudentDto::new)
                .orElse(null);
    }
}

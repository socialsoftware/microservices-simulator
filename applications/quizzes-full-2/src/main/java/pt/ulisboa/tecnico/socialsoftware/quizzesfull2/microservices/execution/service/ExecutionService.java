package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExecutionService {
    private final ExecutionCustomRepository executionCustomRepository;
    private final ExecutionFactory executionFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public ExecutionService(ExecutionCustomRepository executionCustomRepository,
                            ExecutionFactory executionFactory,
                            UnitOfWorkService unitOfWorkService,
                            AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.executionCustomRepository = executionCustomRepository;
        this.executionFactory = executionFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto getExecutionById(Integer executionAggregateId, UnitOfWork unitOfWork) {
        return executionFactory.createExecutionDto(
                (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<ExecutionDto> getExecutions(UnitOfWork unitOfWork) {
        return executionCustomRepository.findAllExecutionIds().stream()
                .map(executionAggregateId -> executionFactory.createExecutionDto(
                        (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<ExecutionDto> getUserExecutions(Integer userAggregateId, UnitOfWork unitOfWork) {
        return executionCustomRepository.findExecutionIdsByStudent(userAggregateId).stream()
                .map(executionAggregateId -> executionFactory.createExecutionDto(
                        (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto createExecution(ExecutionDto executionDto, CourseDto courseDto, UnitOfWork unitOfWork) {
        checkNoDuplicateCourseExecution(executionDto.getAcronym(), executionDto.getAcademicTerm(), unitOfWork);

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Execution execution = executionFactory.createExecution(aggregateId, courseDto.getAggregateId(),
                courseDto.getName(), courseDto.getType(), executionDto.getAcronym(),
                executionDto.getAcademicTerm(), executionDto.getEndDate());

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
    public void enrollStudent(Integer executionAggregateId, UserDto userDto, UnitOfWork unitOfWork) {
        if (!Boolean.TRUE.equals(userDto.isActive())) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.INACTIVE_USER);
        }

        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);

        newExecution.addStudent(new ExecutionStudent(userDto.getAggregateId(), userDto.getName(),
                userDto.getUsername(), userDto.getVersion(), userDto.isActive()));

        unitOfWorkService.registerChanged(newExecution, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void disenrollStudent(Integer executionAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);

        newExecution.removeStudent(userAggregateId);

        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(
                new DisenrollStudentFromCourseExecutionEvent(executionAggregateId, userAggregateId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionCopy(oldExecution);

        newExecution.setStudents(new ArrayList<>());
        newExecution.remove();

        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteCourseExecutionEvent(executionAggregateId), unitOfWork);
    }

    private void checkNoDuplicateCourseExecution(String acronym, String academicTerm, UnitOfWork unitOfWork) {
        for (Integer executionAggregateId : executionCustomRepository.findAllExecutionIds()) {
            Execution existing = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
                    executionAggregateId, unitOfWork);
            if (existing.getAcronym().equals(acronym) && existing.getAcademicTerm().equals(academicTerm)) {
                throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.NO_DUPLICATE_COURSE_EXECUTION);
            }
        }
    }
}

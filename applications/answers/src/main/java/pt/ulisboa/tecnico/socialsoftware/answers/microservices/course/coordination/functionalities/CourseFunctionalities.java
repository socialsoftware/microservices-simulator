package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto;
import java.util.List;

@Service
public class CourseFunctionalities {
    @Autowired
    private CourseService courseService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CourseDto createCourse(CreateCourseRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateCourseFunctionalitySagas createCourseFunctionalitySagas = new CreateCourseFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createCourseFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createCourseFunctionalitySagas.getCreatedCourseDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CourseDto getCourseById(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseByIdFunctionalitySagas getCourseByIdFunctionalitySagas = new GetCourseByIdFunctionalitySagas(
                        sagaUnitOfWorkService, courseAggregateId, sagaUnitOfWork, commandGateway);
                getCourseByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCourseByIdFunctionalitySagas.getCourseDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CourseDto updateCourse(CourseDto courseDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(courseDto);
                UpdateCourseFunctionalitySagas updateCourseFunctionalitySagas = new UpdateCourseFunctionalitySagas(
                        sagaUnitOfWorkService, courseDto, sagaUnitOfWork, commandGateway);
                updateCourseFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateCourseFunctionalitySagas.getUpdatedCourseDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteCourse(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteCourseFunctionalitySagas deleteCourseFunctionalitySagas = new DeleteCourseFunctionalitySagas(
                        sagaUnitOfWorkService, courseAggregateId, sagaUnitOfWork, commandGateway);
                deleteCourseFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CourseDto> getAllCourses() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllCoursesFunctionalitySagas getAllCoursesFunctionalitySagas = new GetAllCoursesFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllCoursesFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllCoursesFunctionalitySagas.getCourses();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(CourseDto courseDto) {
        if (courseDto.getName() == null) {
            throw new AnswersException(COURSE_MISSING_NAME);
        }
}

    private void checkInput(CreateCourseRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new AnswersException(COURSE_MISSING_NAME);
        }
}
}
package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateCourseRequestDto;
import java.util.List;

@Service
public class CourseFunctionalities {
    @Autowired
    private CourseService courseService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


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
                        sagaUnitOfWork, sagaUnitOfWorkService, courseService, createRequest);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, courseService, courseAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, courseService, courseDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, courseService, courseAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, courseService);
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
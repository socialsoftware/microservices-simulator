package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception.CrossrefsErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception.CrossrefsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.teacher.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.webapi.requestDtos.CreateTeacherRequestDto;
import java.util.List;

@Service
public class TeacherFunctionalities {
    @Autowired
    private TeacherService teacherService;

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
            throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TeacherDto createTeacher(CreateTeacherRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateTeacherFunctionalitySagas createTeacherFunctionalitySagas = new CreateTeacherFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, teacherService, createRequest);
                createTeacherFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTeacherFunctionalitySagas.getCreatedTeacherDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TeacherDto getTeacherById(Integer teacherAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTeacherByIdFunctionalitySagas getTeacherByIdFunctionalitySagas = new GetTeacherByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, teacherService, teacherAggregateId);
                getTeacherByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTeacherByIdFunctionalitySagas.getTeacherDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TeacherDto updateTeacher(TeacherDto teacherDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(teacherDto);
                UpdateTeacherFunctionalitySagas updateTeacherFunctionalitySagas = new UpdateTeacherFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, teacherService, teacherDto);
                updateTeacherFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTeacherFunctionalitySagas.getUpdatedTeacherDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTeacher(Integer teacherAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTeacherFunctionalitySagas deleteTeacherFunctionalitySagas = new DeleteTeacherFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, teacherService, teacherAggregateId);
                deleteTeacherFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TeacherDto> getAllTeachers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllTeachersFunctionalitySagas getAllTeachersFunctionalitySagas = new GetAllTeachersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, teacherService);
                getAllTeachersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllTeachersFunctionalitySagas.getTeachers();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(TeacherDto teacherDto) {
        if (teacherDto.getName() == null) {
            throw new CrossrefsException(TEACHER_MISSING_NAME);
        }
        if (teacherDto.getEmail() == null) {
            throw new CrossrefsException(TEACHER_MISSING_EMAIL);
        }
        if (teacherDto.getDepartment() == null) {
            throw new CrossrefsException(TEACHER_MISSING_DEPARTMENT);
        }
}

    private void checkInput(CreateTeacherRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new CrossrefsException(TEACHER_MISSING_NAME);
        }
        if (createRequest.getEmail() == null) {
            throw new CrossrefsException(TEACHER_MISSING_EMAIL);
        }
        if (createRequest.getDepartment() == null) {
            throw new CrossrefsException(TEACHER_MISSING_DEPARTMENT);
        }
}
}
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
import pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.webapi.requestDtos.CreateEnrollmentRequestDto;
import java.util.List;

@Service
public class EnrollmentFunctionalities {
    @Autowired
    private EnrollmentService enrollmentService;

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

    public EnrollmentDto createEnrollment(CreateEnrollmentRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateEnrollmentFunctionalitySagas createEnrollmentFunctionalitySagas = new CreateEnrollmentFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService, createRequest);
                createEnrollmentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createEnrollmentFunctionalitySagas.getCreatedEnrollmentDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public EnrollmentDto getEnrollmentById(Integer enrollmentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetEnrollmentByIdFunctionalitySagas getEnrollmentByIdFunctionalitySagas = new GetEnrollmentByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService, enrollmentAggregateId);
                getEnrollmentByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getEnrollmentByIdFunctionalitySagas.getEnrollmentDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public EnrollmentDto updateEnrollment(EnrollmentDto enrollmentDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(enrollmentDto);
                UpdateEnrollmentFunctionalitySagas updateEnrollmentFunctionalitySagas = new UpdateEnrollmentFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService, enrollmentDto);
                updateEnrollmentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateEnrollmentFunctionalitySagas.getUpdatedEnrollmentDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteEnrollment(Integer enrollmentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteEnrollmentFunctionalitySagas deleteEnrollmentFunctionalitySagas = new DeleteEnrollmentFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService, enrollmentAggregateId);
                deleteEnrollmentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<EnrollmentDto> getAllEnrollments() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllEnrollmentsFunctionalitySagas getAllEnrollmentsFunctionalitySagas = new GetAllEnrollmentsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService);
                getAllEnrollmentsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllEnrollmentsFunctionalitySagas.getEnrollments();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public EnrollmentTeacherDto addEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddEnrollmentTeacherFunctionalitySagas addEnrollmentTeacherFunctionalitySagas = new AddEnrollmentTeacherFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService,
                        enrollmentId, teacherAggregateId, teacherDto);
                addEnrollmentTeacherFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addEnrollmentTeacherFunctionalitySagas.getAddedTeacherDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<EnrollmentTeacherDto> addEnrollmentTeachers(Integer enrollmentId, List<EnrollmentTeacherDto> teacherDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddEnrollmentTeachersFunctionalitySagas addEnrollmentTeachersFunctionalitySagas = new AddEnrollmentTeachersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService,
                        enrollmentId, teacherDtos);
                addEnrollmentTeachersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addEnrollmentTeachersFunctionalitySagas.getAddedTeacherDtos();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public EnrollmentTeacherDto getEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetEnrollmentTeacherFunctionalitySagas getEnrollmentTeacherFunctionalitySagas = new GetEnrollmentTeacherFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService,
                        enrollmentId, teacherAggregateId);
                getEnrollmentTeacherFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getEnrollmentTeacherFunctionalitySagas.getTeacherDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public EnrollmentTeacherDto updateEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateEnrollmentTeacherFunctionalitySagas updateEnrollmentTeacherFunctionalitySagas = new UpdateEnrollmentTeacherFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService,
                        enrollmentId, teacherAggregateId, teacherDto);
                updateEnrollmentTeacherFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateEnrollmentTeacherFunctionalitySagas.getUpdatedTeacherDto();
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveEnrollmentTeacherFunctionalitySagas removeEnrollmentTeacherFunctionalitySagas = new RemoveEnrollmentTeacherFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, enrollmentService,
                        enrollmentId, teacherAggregateId);
                removeEnrollmentTeacherFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new CrossrefsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(EnrollmentDto enrollmentDto) {
}

    private void checkInput(CreateEnrollmentRequestDto createRequest) {
}
}
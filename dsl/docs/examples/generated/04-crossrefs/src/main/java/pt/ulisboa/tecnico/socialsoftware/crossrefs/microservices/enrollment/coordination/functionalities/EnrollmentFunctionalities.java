package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.webapi.requestDtos.CreateEnrollmentRequestDto;
import java.util.List;

@Service
public class EnrollmentFunctionalities {
    @Autowired
    private EnrollmentService enrollmentService;

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
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, enrollmentAggregateId, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, enrollmentDto, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, enrollmentAggregateId, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService,
                        enrollmentId, teacherAggregateId, teacherDto,
                        sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService,
                        enrollmentId, teacherDtos,
                        sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService,
                        enrollmentId, teacherAggregateId,
                        sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService,
                        enrollmentId, teacherAggregateId, teacherDto,
                        sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService,
                        enrollmentId, teacherAggregateId,
                        sagaUnitOfWork, commandGateway);
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
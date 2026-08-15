package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.CreateCourseFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.DeleteCourseFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.GetCourseByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.UpdateCourseFunctionalitySagas;

@Service
public class CourseFunctionalities {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public CourseDto getCourseById(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetCourseByIdFunctionalitySagas saga = new GetCourseByIdFunctionalitySagas(
                unitOfWorkService, courseAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCourseDto();
    }

    public CourseDto createCourse(String name, String type) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateCourseFunctionalitySagas saga = new CreateCourseFunctionalitySagas(
                unitOfWorkService, name, type, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedCourseDto();
    }

    public void updateCourse(Integer courseAggregateId, String name, String type) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateCourseFunctionalitySagas saga = new UpdateCourseFunctionalitySagas(
                unitOfWorkService, courseAggregateId, name, type, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteCourse(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteCourseFunctionalitySagas saga = new DeleteCourseFunctionalitySagas(
                unitOfWorkService, courseAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}

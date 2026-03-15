package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.service;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

@Service
public class CourseExecutionService {

    private final SagaUnitOfWorkService unitOfWorkService;

    public CourseExecutionService(SagaUnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public Object getCourseExecutionById(Integer id, SagaUnitOfWork unitOfWork) {
        return null;
    }

    public Object createCourseExecution(SagaUnitOfWork unitOfWork) {
        unitOfWorkService.registerChanged(null, unitOfWork);
        return null;
    }
}

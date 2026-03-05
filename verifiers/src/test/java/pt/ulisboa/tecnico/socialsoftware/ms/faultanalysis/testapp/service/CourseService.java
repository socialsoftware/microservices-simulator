package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

@Service
public class CourseService {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    public Object getCourseById(Integer id, SagaUnitOfWork unitOfWork) {
        return null;
    }

    public Object createCourse(SagaUnitOfWork unitOfWork) {
        unitOfWorkService.registerChanged(null, unitOfWork);
        return null;
    }
}

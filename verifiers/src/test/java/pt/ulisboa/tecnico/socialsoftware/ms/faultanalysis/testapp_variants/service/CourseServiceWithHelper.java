package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp_variants.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

// S1: registerChanged is inside a private helper method, not directly in the public method.
// ServiceVisitor uses findAll() on the public method's AST subtree, but the private
// method body is a sibling node — not a child — so it is invisible to the search.
@Service
public class CourseServiceWithHelper {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    public Object createCourse(SagaUnitOfWork unitOfWork) {
        return doCreate(unitOfWork);
    }

    private Object doCreate(SagaUnitOfWork unitOfWork) {
        unitOfWorkService.registerChanged(null, unitOfWork);
        return null;
    }
}

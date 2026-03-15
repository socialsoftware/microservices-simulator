package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.functionalities;

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.dto.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.saga.CreateCourseSaga;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

/**
 * Wraps saga creation behind a convenience method.
 * Used to test SpockTestVisitor's indirect saga resolution via SagaCreationSiteVisitor.
 */
public class CourseFunctionalities {

    private SagaUnitOfWorkService unitOfWorkService;

    public CreateCourseSaga createCourse(Integer courseAggregateId, CourseDto courseDto,
                                         SagaUnitOfWork unitOfWork) {
        return new CreateCourseSaga(unitOfWorkService, courseAggregateId, courseDto, unitOfWork);
    }
}

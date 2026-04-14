package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class EnrollmentEventProcessing {
    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentFactory enrollmentFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public EnrollmentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCourseDeletedEvent(Integer aggregateId, CourseDeletedEvent courseDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Enrollment oldEnrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Enrollment newEnrollment = enrollmentFactory.createEnrollmentFromExisting(oldEnrollment);
        newEnrollment.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newEnrollment, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
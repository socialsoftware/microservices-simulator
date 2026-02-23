package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.publish.CourseDeletedEvent;

@Service
public class EnrollmentEventProcessing {
    @Autowired
    private EnrollmentService enrollmentService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public EnrollmentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCourseDeletedEvent(Integer aggregateId, CourseDeletedEvent courseDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}
package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.TeacherDeletedEvent;

@Service
public class CourseEventProcessing {
    @Autowired
    private CourseService courseService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public CourseEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processTeacherDeletedEvent(Integer aggregateId, TeacherDeletedEvent teacherDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}
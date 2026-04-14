package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.TeacherDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class CourseEventProcessing {
    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseFactory courseFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public CourseEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processTeacherDeletedEvent(Integer aggregateId, TeacherDeletedEvent teacherDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
        newCourse.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newCourse, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
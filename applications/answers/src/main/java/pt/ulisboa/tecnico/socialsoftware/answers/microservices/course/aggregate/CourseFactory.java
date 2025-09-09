package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class CourseFactory {

    public Course createCourse(Integer aggregateId, CourseDto courseDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new Course(
            courseDto.getName(),
            courseDto.getAcronym(),
            courseDto.getCourseType(),
            courseDto.getCreationDate()
        );
    }

    public Course createCourseFromExisting(Course existingCourse) {
        // Create a copy of the existing aggregate
        if (existingCourse instanceof Course) {
            return new Course((Course) existingCourse);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public CourseDto createCourseDto(Course course) {
        return new CourseDto((Course) course);
    }
}
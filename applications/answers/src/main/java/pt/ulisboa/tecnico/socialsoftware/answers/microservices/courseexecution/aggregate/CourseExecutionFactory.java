package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class CourseExecutionFactory {

    public CourseExecution createCourseExecution(Integer aggregateId, CourseExecutionDto courseexecutionDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new CourseExecution(
            courseexecutionDto.getName(),
            courseexecutionDto.getAcronym(),
            courseexecutionDto.getAcademicTerm(),
            courseexecutionDto.getStartDate(),
            courseexecutionDto.getEndDate(),
            courseexecutionDto.getCourse(),
            courseexecutionDto.getStudents()
        );
    }

    public CourseExecution createCourseExecutionFromExisting(CourseExecution existingCourseExecution) {
        // Create a copy of the existing aggregate
        if (existingCourseExecution instanceof CourseExecution) {
            return new CourseExecution((CourseExecution) existingCourseExecution);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public CourseExecutionDto createCourseExecutionDto(CourseExecution courseexecution) {
        return new CourseExecutionDto((CourseExecution) courseexecution);
    }
}
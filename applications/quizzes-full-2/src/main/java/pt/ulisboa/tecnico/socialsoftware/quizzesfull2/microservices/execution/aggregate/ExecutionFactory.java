package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType;

import java.time.LocalDateTime;

public interface ExecutionFactory {
    Execution createExecution(Integer aggregateId, Integer courseAggregateId, String courseName, CourseType courseType,
                              String acronym, String academicTerm, LocalDateTime endDate);

    Execution createExecutionCopy(Execution existing);

    ExecutionDto createExecutionDto(Execution execution);
}

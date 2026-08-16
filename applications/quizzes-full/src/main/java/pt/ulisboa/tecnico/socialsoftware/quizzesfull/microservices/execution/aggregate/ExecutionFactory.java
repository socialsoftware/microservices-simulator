package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate;

public interface ExecutionFactory {
    Execution createExecution(Integer aggregateId, String acronym, String academicTerm, ExecutionCourse executionCourse);
    Execution createExecutionCopy(Execution existing);
    ExecutionDto createExecutionDto(Execution execution);
}

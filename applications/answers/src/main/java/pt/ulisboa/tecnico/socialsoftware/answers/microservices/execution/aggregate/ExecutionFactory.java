package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class ExecutionFactory {

    public Execution createExecution(Integer aggregateId, ExecutionDto executionDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new Execution(
            executionDto.getAcronym(),
            executionDto.getAcademicTerm(),
            executionDto.getEndDate(),
            executionDto.getExecutionCourse(),
            executionDto.getStudents()
        );
    }

    public Execution createExecutionFromExisting(Execution existingExecution) {
        // Create a copy of the existing aggregate
        if (existingExecution instanceof Execution) {
            return new Execution((Execution) existingExecution);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public ExecutionDto createExecutionDto(Execution execution) {
        return new ExecutionDto((Execution) execution);
    }
}
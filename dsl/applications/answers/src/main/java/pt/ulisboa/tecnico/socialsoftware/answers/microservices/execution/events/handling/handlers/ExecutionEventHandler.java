package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionRepository;

public abstract class ExecutionEventHandler extends EventHandler {
    private ExecutionRepository executionRepository;
    protected ExecutionEventProcessing executionEventProcessing;

    public ExecutionEventHandler(ExecutionRepository executionRepository, ExecutionEventProcessing executionEventProcessing) {
        this.executionRepository = executionRepository;
        this.executionEventProcessing = executionEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return executionRepository.findAll().stream().map(Execution::getAggregateId).collect(Collectors.toSet());
    }

}

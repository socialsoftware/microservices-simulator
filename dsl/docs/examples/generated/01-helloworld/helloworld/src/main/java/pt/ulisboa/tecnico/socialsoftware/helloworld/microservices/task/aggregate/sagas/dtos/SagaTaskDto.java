package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.Task;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.Task;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.SagaTask;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaTaskDto extends TaskDto {
private SagaState sagaState;

public SagaTaskDto(Task task) {
super((Task) task);
this.sagaState = ((SagaTask)task).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}
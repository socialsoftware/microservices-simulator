package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.sagas.SagaPost;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaPostDto extends PostDto {
private SagaState sagaState;

public SagaPostDto(Post post) {
super((Post) post);
this.sagaState = ((SagaPost)post).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}
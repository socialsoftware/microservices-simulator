package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.CreateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private final List<QuestionTopicDto> topics = new ArrayList<>();

    public CreateQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, QuestionDto questionDto,
                                            List<Integer> topicAggregateIds, SagaUnitOfWork unitOfWork,
                                            CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, questionDto, topicAggregateIds, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, QuestionDto questionDto,
                              List<Integer> topicAggregateIds, SagaUnitOfWork unitOfWork,
                              CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: the fetch itself is the check — it throws when the course does not
        // exist, so no explicit guard is written in QuestionService. Question caches no Course
        // field beyond courseAggregateId, so the fetched DTO is not carried forward.
        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand cmd = new GetCourseByIdCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), questionDto.getCourseAggregateId());
            commandGateway.send(cmd);
        });

        // P4a prerequisite: seeds each QuestionTopic snapshot, including the courseAggregateId that
        // TOPIC_BELONGS_TO_QUESTION_COURSE is checked against at P1 in Question.verifyInvariants().
        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            for (Integer topicAggregateId : topicAggregateIds) {
                GetTopicByIdCommand cmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
                TopicDto topicDto = (TopicDto) commandGateway.send(cmd);
                this.topics.add(new QuestionTopicDto(topicDto.getAggregateId(), topicDto.getName(),
                        topicDto.getVersion(), topicDto.getCourseAggregateId()));
            }
        });

        SagaStep createQuestionStep = new SagaStep("createQuestionStep", () -> {
            CreateQuestionCommand cmd = new CreateQuestionCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto, this.topics);
            this.questionDto = (QuestionDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getCourseStep, getTopicsStep)));

        this.workflow.addStep(getCourseStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(createQuestionStep);
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }
}

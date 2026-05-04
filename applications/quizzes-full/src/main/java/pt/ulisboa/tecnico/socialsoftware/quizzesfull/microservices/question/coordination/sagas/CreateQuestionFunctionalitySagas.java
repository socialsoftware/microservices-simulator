package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.IncrementQuestionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.CreateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto courseDto;
    private Set<QuestionTopic> questionTopics;
    private QuestionDto createdQuestionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            String title, String content,
                                            Integer courseAggregateId, List<Integer> topicIds,
                                            Set<Option> options,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(title, content, courseAggregateId, topicIds, options, unitOfWork);
    }

    public void buildWorkflow(String title, String content,
                              Integer courseAggregateId, List<Integer> topicIds,
                              Set<Option> options,
                              SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCourseByIdCommand);
            sagaCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            this.courseDto = (CourseDto) commandGateway.send(sagaCommand);
        });

        getCourseStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            this.questionTopics = new HashSet<>();
            for (Integer topicId : topicIds) {
                GetTopicByIdCommand getTopicCmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                TopicDto topicDto = (TopicDto) commandGateway.send(getTopicCmd);
                this.questionTopics.add(new QuestionTopic(topicDto));
            }
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        SagaStep createQuestionStep = new SagaStep("createQuestionStep", () -> {
            QuestionCourse questionCourse = new QuestionCourse(this.courseDto);
            CreateQuestionCommand createQuestionCommand = new CreateQuestionCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    title, content, questionCourse, options, this.questionTopics);
            this.createdQuestionDto = (QuestionDto) commandGateway.send(createQuestionCommand);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        SagaStep incrementCourseQuestionCountStep = new SagaStep("incrementCourseQuestionCountStep", () -> {
            IncrementQuestionCountCommand incrementCommand = new IncrementQuestionCountCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            commandGateway.send(incrementCommand);
        }, new ArrayList<>(Arrays.asList(createQuestionStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(createQuestionStep);
        workflow.addStep(incrementCourseQuestionCountStep);
    }

    public QuestionDto getCreatedQuestionDto() { return createdQuestionDto; }
}

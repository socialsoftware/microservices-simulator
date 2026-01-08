package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;
import java.util.Set;
import java.util.HashSet;

public class CreateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto createdQuestionDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private SagaCourseDto courseDto;
    private QuestionCourse course;
    private final CourseService courseService;
    private final TopicService topicService;


    public CreateQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, CourseService courseService, TopicService topicService, QuestionDto questionDto) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.courseService = courseService;
        this.topicService = topicService;
        this.buildWorkflow(questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseStep = new SagaSyncStep("getCourseStep", () -> {
            Integer courseAggregateId = questionDto.getCourseAggregateId();
            courseDto = (SagaCourseDto) courseService.getCourseById(courseAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), CourseSagaState.READ_COURSE, unitOfWork);
            QuestionCourse course = new QuestionCourse(courseDto);
            setCourse(course);
        });

        getCourseStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep createQuestionStep = new SagaSyncStep("createQuestionStep", () -> {
            Set<QuestionTopic> topics = null;
            if (questionDto.getTopicsAggregateIds() != null) {
                topics = new HashSet<>();
                for (Integer topicAggregateId : questionDto.getTopicsAggregateIds()) {
                    SagaTopicDto topicDto = (SagaTopicDto) topicService.getTopicById(topicAggregateId, unitOfWork);
                    topics.add(new QuestionTopic(topicDto));
                }
            }
            QuestionDto createdQuestionDto = questionService.createQuestion(getCourse(), questionDto, topics, unitOfWork);
            setCreatedQuestionDto(createdQuestionDto);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(createQuestionStep);

    }

    public QuestionCourse getCourse() {
        return course;
    }

    public void setCourse(QuestionCourse course) {
        this.course = course;
    }

    public QuestionDto getCreatedQuestionDto() {
        return createdQuestionDto;
    }

    public void setCreatedQuestionDto(QuestionDto createdQuestionDto) {
        this.createdQuestionDto = createdQuestionDto;
    }
}

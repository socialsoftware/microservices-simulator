package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.question;

import java.util.List;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;

public class CreateQuestionFunctionalityTCC extends WorkflowFunctionality {
    private QuestionCourse course;
    private List<TopicDto> topics;
    private QuestionDto createdQuestion;
    private final QuestionService questionService;
    private final TopicService topicService;
    private final CourseService courseService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public CreateQuestionFunctionalityTCC(QuestionService questionService, TopicService topicService, CourseService courseService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, QuestionDto questionDto, CausalUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.topicService = topicService;
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, QuestionDto questionDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
             QuestionCourse course = new QuestionCourse(courseService.getCourseById(courseAggregateId, unitOfWork));
            /*
                COURSE_SAME_TOPICS_COURSE
            */

            for (TopicDto topicDto : questionDto.getTopicDto()) {
                if (!topicDto.getCourseId().equals(courseAggregateId)) {
                    throw new TutorException(ErrorMessage.QUESTION_TOPIC_INVALID_COURSE, topicDto.getAggregateId(), courseAggregateId);
                }
            }

            List<TopicDto> topics = questionDto.getTopicDto().stream()
                    .map(topicDto -> topicService.getTopicById(topicDto.getAggregateId(), unitOfWork))
                    .collect(Collectors.toList());

            this.createdQuestion = questionService.createQuestion(course, questionDto, topics, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public QuestionCourse getCourse() {
        return course;
    }

    public void setCourse(QuestionCourse course) {
        this.course = course;
    }

    public List<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDto> topics) {
        this.topics = topics;
    }

    public QuestionDto getCreatedQuestion() {
        return createdQuestion;
    }

    public void setCreatedQuestion(QuestionDto createdQuestion) {
        this.createdQuestion = createdQuestion;
    }
}
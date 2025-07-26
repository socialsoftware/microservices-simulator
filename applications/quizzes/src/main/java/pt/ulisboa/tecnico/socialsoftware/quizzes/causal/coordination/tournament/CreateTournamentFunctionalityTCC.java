package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateTournamentFunctionalityTCC extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private UserDto userDto;
    private HashSet<TopicDto> topicDtos = new HashSet<TopicDto>();
    private QuizDto quizDto;
    private TournamentDto tournamentDto;
    private final TournamentService tournamentService;
    private final CourseExecutionService courseExecutionService;
    private final TopicService topicService;
    private final QuizService quizService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public CreateTournamentFunctionalityTCC(TournamentService tournamentService, CourseExecutionService courseExecutionService, TopicService topicService, QuizService quizService, CausalUnitOfWorkService unitOfWorkService, 
                                Integer userId, Integer executionId, List<Integer> topicsId, TournamentDto tournamentDto, 
                                CausalUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.topicService = topicService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userId, executionId, topicsId, tournamentDto, unitOfWork);
    }

    public void buildWorkflow(Integer userId, Integer executionId, List<Integer> topicsId,
                                          TournamentDto tournamentDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // by making this call the invariants regarding the course execution and the role of the creator are guaranteed
            UserDto creatorDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionId, userId, unitOfWork);

            CourseExecutionDto courseExecutionDto = courseExecutionService.getCourseExecutionById(executionId, unitOfWork);

            Set<TopicDto> topicDtos = topicsId.stream()
                    .map(topicId -> topicService.getTopicById(topicId, unitOfWork))
                    .collect(Collectors.toSet());

            QuizDto quizDto = new QuizDto();
            quizDto.setAvailableDate(tournamentDto.getStartTime());
            quizDto.setConclusionDate(tournamentDto.getEndTime());
            quizDto.setResultsDate(tournamentDto.getEndTime());
            QuizDto quizResultDto = quizService.generateQuiz(executionId, quizDto, topicsId, tournamentDto.getNumberOfQuestions(), unitOfWork);

            //        NUMBER_OF_QUESTIONS
            //            this.numberOfQuestions == Quiz(tournamentQuiz.id).quizQuestions.size
            //            Quiz(this.tournamentQuiz.id) DEPENDS ON this.numberOfQuestions
            //        QUIZ_TOPICS
            //            Quiz(this.tournamentQuiz.id) DEPENDS ON this.topics // the topics of the quiz questions are related to the tournament topics
            //        START_TIME_AVAILABLE_DATE
            //            this.startTime == Quiz(tournamentQuiz.id).availableDate
            //        END_TIME_CONCLUSION_DATE
            //            this.endTime == Quiz(tournamentQuiz.id).conclusionDate

            this.tournamentDto = tournamentService.createTournament(tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizResultDto, unitOfWork);
        });
                                
        workflow.addStep(step);
    }
    

    public void setCourseExecutionDto(CourseExecutionDto courseExecutionDto) {
        this.courseExecutionDto = courseExecutionDto;
    }

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public HashSet<TopicDto> getTopicsDtos() {
        return topicDtos;
    }

    public void setTopicsDtos(HashSet<TopicDto> topicDtos) {
        this.topicDtos = topicDtos;
    }

    public void addTopicDto(TopicDto topicDto) {
        this.topicDtos.add(topicDto);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public TournamentDto getTournamentDto() {
        return this.tournamentDto;
    }

    public void setTournamentDto(TournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
    }

}
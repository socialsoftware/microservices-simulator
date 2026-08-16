package pt.ulisboa.tecnico.socialsoftware.quizzes.executor;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioPrerequisiteProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioPrerequisiteResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioRuntimeContext;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class QuizzesStaleReadPrerequisiteProvider implements ScenarioPrerequisiteProvider {
    public static final String PROVIDER_ID = "quizzes-stale-read-baseline";
    public static final String PROVIDER_VERSION = "1";
    public static final String UPDATED_NAME = "UpdatedName";

    private Map<String, Object> preparedBindings;
    private Integer preparedQuizAggregateId;

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public String providerVersion() {
        return PROVIDER_VERSION;
    }

    @Override
    public synchronized ScenarioPrerequisiteResult prepare(ScenarioRuntimeContext runtimeContext,
                                                           List<BaselineBindingRequirement> requiredBindings) {
        if (preparedBindings != null) {
            return result(preparedBindings, requiredBindings, "reused");
        }

        ExecutionFunctionalities executions = bean(runtimeContext, ExecutionFunctionalities.class);
        UserFunctionalities users = bean(runtimeContext, UserFunctionalities.class);
        TopicFunctionalities topics = bean(runtimeContext, TopicFunctionalities.class);
        QuestionFunctionalities questions = bean(runtimeContext, QuestionFunctionalities.class);
        TournamentFunctionalities tournaments = bean(runtimeContext, TournamentFunctionalities.class);

        CourseExecutionDto execution = new CourseExecutionDto();
        execution.setName("BLCM");
        execution.setType("TECNICO");
        execution.setAcronym("TESTBLCM");
        execution.setAcademicTerm("2022/2023");
        execution.setEndDate(DateHandler.toISOString(DateHandler.now().plusHours(2)));
        execution = executions.createCourseExecution(execution);

        UserDto creator = new UserDto();
        creator.setName("USER_NAME_1");
        creator.setUsername("USER_USERNAME_1");
        creator.setRole("STUDENT");
        creator = users.createUser(creator);
        users.activateUser(creator.getAggregateId());
        executions.addStudent(execution.getAggregateId(), creator.getAggregateId());

        TopicDto firstTopic = new TopicDto();
        firstTopic.setName("TOPIC_NAME_1");
        firstTopic = topics.createTopic(execution.getCourseAggregateId(), firstTopic);
        TopicDto secondTopic = new TopicDto();
        secondTopic.setName("TOPIC_NAME_2");
        secondTopic = topics.createTopic(execution.getCourseAggregateId(), secondTopic);
        createQuestion(questions, execution.getCourseAggregateId(), firstTopic,
                "Title One", "Content One", "Option One", "Option Two");
        createQuestion(questions, execution.getCourseAggregateId(), secondTopic,
                "Title Two", "Content Two", "Option Three", "Option Four");

        TournamentDto tournament = new TournamentDto();
        tournament.setStartTime(DateHandler.toISOString(DateHandler.now().plusMinutes(5)));
        tournament.setEndTime(DateHandler.toISOString(DateHandler.now().plusHours(1)));
        tournament.setNumberOfQuestions(2);
        tournament = tournaments.createTournament(
                creator.getAggregateId(), execution.getAggregateId(),
                List.of(firstTopic.getAggregateId(), secondTopic.getAggregateId()), tournament);

        UserDto updatedUser = new UserDto();
        updatedUser.setName(UPDATED_NAME);

        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("courseExecutionAggregateId", execution.getAggregateId());
        bindings.put("creatorUserAggregateId", creator.getAggregateId());
        bindings.put("tournamentAggregateId", tournament.getAggregateId());
        bindings.put("updatedUser", updatedUser);

        preparedBindings = Map.copyOf(bindings);
        preparedQuizAggregateId = tournament.getQuiz().getAggregateId();
        return result(preparedBindings, requiredBindings, "created");
    }

    private ScenarioPrerequisiteResult result(Map<String, Object> bindings,
                                              List<BaselineBindingRequirement> requiredBindings,
                                              String baselineInstance) {
        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("baseline", "creator enrolled in course execution and owns participant-free tournament");
        evidence.put("baselineInstance", baselineInstance);
        evidence.put("courseExecutionAggregateId", bindings.get("courseExecutionAggregateId").toString());
        evidence.put("participantUserAggregateId", bindings.get("creatorUserAggregateId").toString());
        evidence.put("referencedQuizAggregateId", preparedQuizAggregateId.toString());
        evidence.put("tournamentAggregateId", bindings.get("tournamentAggregateId").toString());
        evidence.put("provider", PROVIDER_ID + "@" + PROVIDER_VERSION);
        evidence.put("requiredBindingCount", Integer.toString(requiredBindings.size()));
        return new ScenarioPrerequisiteResult(bindings, evidence);
    }

    private QuestionDto createQuestion(QuestionFunctionalities questions,
                                       Integer courseAggregateId,
                                       TopicDto topic,
                                       String title,
                                       String content,
                                       String firstOption,
                                       String secondOption) {
        QuestionDto question = new QuestionDto();
        question.setTitle(title);
        question.setContent(content);
        question.setTopicDto(new LinkedHashSet<>(Set.of(topic)));
        OptionDto first = new OptionDto();
        first.setSequence(1);
        first.setCorrect(true);
        first.setContent(firstOption);
        OptionDto second = new OptionDto();
        second.setSequence(2);
        second.setCorrect(false);
        second.setContent(secondOption);
        question.setOptionDtos(List.of(first, second));
        return questions.createQuestion(courseAggregateId, question);
    }

    private <T> T bean(ScenarioRuntimeContext runtimeContext, Class<T> type) {
        return type.cast(runtimeContext.bean(type));
    }
}

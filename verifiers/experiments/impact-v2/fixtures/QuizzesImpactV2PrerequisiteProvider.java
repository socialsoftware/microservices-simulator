package pt.ulisboa.tecnico.socialsoftware.quizzes.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioPrerequisiteProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioPrerequisiteResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioRuntimeContext;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Test-classpath-only provider for the bounded persisted ImpactV2 qualification package. */
@Component
public final class QuizzesImpactV2PrerequisiteProvider
        implements ScenarioPrerequisiteProvider, ApplicationListener<ContextClosedEvent> {
    public static final String PROVIDER_ID = "quizzes-impact-v2-qualification";
    public static final String PROVIDER_VERSION = "1";
    private static final String FIXTURE_NOW_PROPERTY = "impact.v2.fixture-now";
    private static final String WITNESS_OUTPUT_PROPERTY = "impact.v2.qualification.witness-output";
    private static final ObjectMapper JSON = new ObjectMapper();

    private Map<String, Object> prepared;
    private Map<String, String> evidence;
    private ScenarioRuntimeContext preparedRuntimeContext;
    private Integer tournamentAggregateId;
    private Integer quizAggregateId;
    private Integer questionAggregateId;

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public String providerVersion() {
        return PROVIDER_VERSION;
    }

    @Override
    public synchronized ScenarioPrerequisiteResult prepare(
            ScenarioRuntimeContext runtimeContext,
            List<BaselineBindingRequirement> requiredBindings) {
        if (prepared == null) {
            prepareOnce(runtimeContext);
        }
        Map<String, String> runEvidence = new LinkedHashMap<>(evidence);
        runEvidence.put("requiredBindingCount", Integer.toString(requiredBindings.size()));
        return new ScenarioPrerequisiteResult(prepared, runEvidence);
    }

    private void prepareOnce(ScenarioRuntimeContext runtimeContext) {
        preparedRuntimeContext = runtimeContext;
        LocalDateTime fixtureNow = LocalDateTime.parse(requiredProperty(FIXTURE_NOW_PROPERTY));
        ExecutionFunctionalities executions = bean(runtimeContext, ExecutionFunctionalities.class);
        UserFunctionalities users = bean(runtimeContext, UserFunctionalities.class);
        TopicFunctionalities topics = bean(runtimeContext, TopicFunctionalities.class);
        QuestionFunctionalities questions = bean(runtimeContext, QuestionFunctionalities.class);
        TournamentFunctionalities tournaments = bean(runtimeContext, TournamentFunctionalities.class);

        CourseExecutionDto execution = new CourseExecutionDto();
        execution.setName("ImpactV2 Qualification");
        execution.setType("TECNICO");
        execution.setAcronym("IMPACTV2");
        execution.setAcademicTerm("2026/2027");
        execution.setEndDate(DateHandler.toISOString(fixtureNow.plusHours(2)));
        execution = executions.createCourseExecution(execution);

        UserDto creator = new UserDto();
        creator.setName("ImpactV2 Creator");
        creator.setUsername("impact-v2-creator");
        creator.setRole("STUDENT");
        creator = users.createUser(creator);
        users.activateUser(creator.getAggregateId());
        executions.addStudent(execution.getAggregateId(), creator.getAggregateId());

        TopicDto first = createTopic(topics, execution.getCourseAggregateId(), "Impact Topic 1");
        TopicDto second = createTopic(topics, execution.getCourseAggregateId(), "Impact Topic 2");
        TopicDto third = createTopic(topics, execution.getCourseAggregateId(), "Impact Topic 3");
        QuestionDto firstQuestion = createQuestion(questions, execution.getCourseAggregateId(), first,
                "Question 1", "Content 1", "A1", "B1");
        createQuestion(questions, execution.getCourseAggregateId(), second,
                "Question 2", "Content 2", "A2", "B2");
        createQuestion(questions, execution.getCourseAggregateId(), third,
                "Question 3", "Content 3", "A3", "B3");

        TournamentDto tournamentInput = new TournamentDto();
        tournamentInput.setStartTime(DateHandler.toISOString(fixtureNow.plusDays(1)));
        tournamentInput.setEndTime(DateHandler.toISOString(fixtureNow.plusDays(2)));
        tournamentInput.setNumberOfQuestions(2);
        TournamentDto tournament = tournaments.createTournament(
                creator.getAggregateId(), execution.getAggregateId(),
                List.of(first.getAggregateId(), second.getAggregateId()), tournamentInput);

        TournamentDto updatedTournament = new TournamentDto();
        updatedTournament.setAggregateId(tournament.getAggregateId());
        updatedTournament.setStartTime(DateHandler.toISOString(fixtureNow.plusDays(1).plusMinutes(25)));
        updatedTournament.setEndTime(DateHandler.toISOString(fixtureNow.plusDays(2).plusMinutes(25)));
        updatedTournament.setNumberOfQuestions(3);

        QuestionDto updatedQuestion = new QuestionDto();
        updatedQuestion.setAggregateId(firstQuestion.getAggregateId());
        updatedQuestion.setTitle("Question 1 updated");
        updatedQuestion.setContent("Content 1 updated");
        updatedQuestion.setOptionDtos(options("A1 updated", "B1 updated"));

        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("tournamentAggregateId", tournament.getAggregateId());
        bindings.put("updatedTournament", updatedTournament);
        bindings.put("updatedTournamentTopicIds", new LinkedHashSet<>(List.of(
                first.getAggregateId(), second.getAggregateId(), third.getAggregateId())));
        bindings.put("updatedQuestion", updatedQuestion);
        prepared = Map.copyOf(bindings);
        tournamentAggregateId = tournament.getAggregateId();
        quizAggregateId = tournament.getQuiz().getAggregateId();
        questionAggregateId = firstQuestion.getAggregateId();

        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("fixtureKind", "qualification-only-provider-backed-not-source-extracted");
        facts.put("fixtureNow", fixtureNow.toString());
        facts.put("courseExecutionAggregateId", execution.getAggregateId().toString());
        facts.put("creatorAggregateId", creator.getAggregateId().toString());
        facts.put("tournamentAggregateId", tournament.getAggregateId().toString());
        facts.put("quizAggregateId", tournament.getQuiz().getAggregateId().toString());
        facts.put("questionAggregateId", firstQuestion.getAggregateId().toString());
        facts.put("provider", PROVIDER_ID + "@" + PROVIDER_VERSION);
        evidence = Map.copyOf(facts);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent ignored) {
        String configuredOutput = System.getProperty(WITNESS_OUTPUT_PROPERTY);
        if (preparedRuntimeContext == null || configuredOutput == null || configuredOutput.isBlank()) {
            return;
        }
        Path output = Path.of(configuredOutput).toAbsolutePath().normalize();
        try {
            EntityManager entityManager = bean(preparedRuntimeContext, EntityManager.class);
            TransactionTemplate transaction = new TransactionTemplate(
                    bean(preparedRuntimeContext, PlatformTransactionManager.class));
            Map<String, Object> witness = transaction.execute(status -> {
                entityManager.clear();
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("schemaVersion", "microservices-simulator.impact-v2-qualification-witness.v1");
                value.put("fixtureKind", "qualification-only-read-only-post-execution-witness");
                value.put("fixtureNow", evidence.get("fixtureNow"));
                value.put("tournament", projection(latest(entityManager, tournamentAggregateId)));
                value.put("quiz", projection(latest(entityManager, quizAggregateId)));
                value.put("question", projection(latest(entityManager, questionAggregateId)));
                return value;
            });
            Files.createDirectories(output.getParent());
            JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), witness);
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to write qualification state witness " + output, failure);
        }
    }

    private static Aggregate latest(EntityManager entityManager, Integer aggregateId) {
        List<Aggregate> rows = entityManager.createQuery(
                        "select a from Aggregate a where a.aggregateId=:id order by a.version desc", Aggregate.class)
                .setParameter("id", aggregateId).setMaxResults(1).getResultList();
        if (rows.isEmpty()) {
            throw new IllegalStateException("Qualification aggregate not found " + aggregateId);
        }
        return rows.getFirst();
    }

    private static Map<String, Object> projection(Aggregate aggregate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aggregateType", aggregate.getAggregateType());
        result.put("aggregateId", aggregate.getAggregateId());
        result.put("version", aggregate.getVersion());
        result.put("lifecycleState", aggregate.getState().name());
        Map<String, Object> data = new LinkedHashMap<>();
        if (aggregate instanceof Tournament tournament) {
            data.put("numberOfQuestions", tournament.getNumberOfQuestions());
            data.put("startTime", tournament.getStartTime().toString());
            data.put("endTime", tournament.getEndTime().toString());
            data.put("topicIds", tournament.getTournamentTopics().stream()
                    .map(value -> value.getTopicAggregateId()).sorted().toList());
            data.put("quizAggregateId", tournament.getTournamentQuiz().getQuizAggregateId());
            data.put("quizVersion", tournament.getTournamentQuiz().getQuizVersion());
        } else if (aggregate instanceof Quiz quiz) {
            List<Map<String, Object>> questions = new ArrayList<>();
            for (QuizQuestion value : quiz.getQuizQuestions().stream()
                    .sorted(Comparator.comparing(QuizQuestion::getQuestionAggregateId)).toList()) {
                Map<String, Object> question = new LinkedHashMap<>();
                question.put("questionAggregateId", value.getQuestionAggregateId());
                question.put("questionVersion", value.getQuestionVersion());
                question.put("title", value.getTitle());
                question.put("content", value.getContent());
                question.put("state", value.getState().name());
                questions.add(question);
            }
            data.put("questions", questions);
        } else if (aggregate instanceof Question question) {
            data.put("title", question.getTitle());
            data.put("content", question.getContent());
            data.put("options", question.getOptions().stream()
                    .sorted(Comparator.comparing(value -> value.getSequence()))
                    .map(value -> Map.of("sequence", value.getSequence(),
                            "correct", value.isCorrect(), "content", value.getContent()))
                    .toList());
        }
        result.put("applicationData", data);
        return result;
    }

    private static TopicDto createTopic(TopicFunctionalities topics, Integer courseId, String name) {
        TopicDto topic = new TopicDto();
        topic.setName(name);
        return topics.createTopic(courseId, topic);
    }

    private static QuestionDto createQuestion(
            QuestionFunctionalities questions,
            Integer courseId,
            TopicDto topic,
            String title,
            String content,
            String firstOption,
            String secondOption) {
        QuestionDto question = new QuestionDto();
        question.setTitle(title);
        question.setContent(content);
        question.setTopicDto(Set.of(topic));
        question.setOptionDtos(options(firstOption, secondOption));
        return questions.createQuestion(courseId, question);
    }

    private static List<OptionDto> options(String firstContent, String secondContent) {
        OptionDto first = new OptionDto();
        first.setSequence(1);
        first.setCorrect(true);
        first.setContent(firstContent);
        OptionDto second = new OptionDto();
        second.setSequence(2);
        second.setCorrect(false);
        second.setContent(secondContent);
        return List.of(first, second);
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required qualification property " + name);
        }
        return value;
    }

    private static <T> T bean(ScenarioRuntimeContext runtimeContext, Class<T> type) {
        return type.cast(runtimeContext.bean(type));
    }
}

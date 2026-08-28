package pt.ulisboa.tecnico.socialsoftware.quizzes.executor;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioSetupActionDispatcher;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Independent closed runtime authorization for Java-confirmed Quizzes setup facade methods. */
@Component
public final class QuizzesSourceSetupActionDispatcher implements ScenarioSetupActionDispatcher {
    public static final String CREATE_COURSE_EXECUTION = key(
            ExecutionFunctionalities.class, "createCourseExecution", CourseExecutionDto.class.getName(),
            CourseExecutionDto.class);
    public static final String CREATE_USER = key(
            UserFunctionalities.class, "createUser", UserDto.class.getName(), UserDto.class);
    public static final String ACTIVATE_USER = key(
            UserFunctionalities.class, "activateUser", Integer.class.getName(), void.class);
    public static final String ADD_STUDENT = key(
            ExecutionFunctionalities.class, "addStudent",
            Integer.class.getName() + "," + Integer.class.getName(), void.class);
    public static final String CREATE_TOPIC = key(
            TopicFunctionalities.class, "createTopic",
            Integer.class.getName() + "," + TopicDto.class.getName(), TopicDto.class);
    public static final String CREATE_QUESTION = key(
            QuestionFunctionalities.class, "createQuestion",
            Integer.class.getName() + "," + QuestionDto.class.getName(), QuestionDto.class);
    public static final String CREATE_TOURNAMENT = key(
            TournamentFunctionalities.class, "createTournament",
            Integer.class.getName() + "," + Integer.class.getName()
                    + ",java.util.List<java.lang.Integer>," + TournamentDto.class.getName(),
            TournamentDto.class);

    private final Map<String, SetupMethod> methods;

    public QuizzesSourceSetupActionDispatcher(ExecutionFunctionalities executions,
                                              UserFunctionalities users,
                                              TopicFunctionalities topics,
                                              QuestionFunctionalities questions,
                                              TournamentFunctionalities tournaments) {
        LinkedHashMap<String, SetupMethod> configured = new LinkedHashMap<>();
        configured.put(CREATE_COURSE_EXECUTION, method(CREATE_COURSE_EXECUTION,
                CourseExecutionDto.class, arguments -> executions.createCourseExecution(
                        argument(arguments, 0, CourseExecutionDto.class))));
        configured.put(CREATE_USER, method(CREATE_USER, UserDto.class,
                arguments -> users.createUser(argument(arguments, 0, UserDto.class))));
        configured.put(ACTIVATE_USER, voidMethod(ACTIVATE_USER, arguments -> {
            users.activateUser(argument(arguments, 0, Integer.class));
            return null;
        }));
        configured.put(ADD_STUDENT, voidMethod(ADD_STUDENT, arguments -> {
            executions.addStudent(argument(arguments, 0, Integer.class),
                    argument(arguments, 1, Integer.class));
            return null;
        }));
        configured.put(CREATE_TOPIC, method(CREATE_TOPIC, TopicDto.class,
                arguments -> topics.createTopic(argument(arguments, 0, Integer.class),
                        argument(arguments, 1, TopicDto.class))));
        configured.put(CREATE_QUESTION, method(CREATE_QUESTION, QuestionDto.class,
                arguments -> questions.createQuestion(argument(arguments, 0, Integer.class),
                        argument(arguments, 1, QuestionDto.class))));
        configured.put(CREATE_TOURNAMENT, method(CREATE_TOURNAMENT, TournamentDto.class,
                arguments -> tournaments.createTournament(
                        argument(arguments, 0, Integer.class),
                        argument(arguments, 1, Integer.class),
                        integerList(arguments, 2),
                        argument(arguments, 3, TournamentDto.class))));
        methods = Map.copyOf(configured);
    }

    @Override
    public Map<String, SetupMethod> setupMethods() {
        return methods;
    }

    private static SetupMethod method(String key,
                                      Class<?> resultType,
                                      Invocation invocation) {
        return new SetupMethod(key, resultType.getName(), false, invocation);
    }

    private static SetupMethod voidMethod(String key, Invocation invocation) {
        return new SetupMethod(key, "void", true, invocation);
    }

    private static <T> T argument(List<Object> arguments, int index, Class<T> type) {
        if (arguments.size() <= index) {
            throw new IllegalArgumentException("missing argument " + index + " for closed setup dispatch");
        }
        return type.cast(arguments.get(index));
    }

    private static List<Integer> integerList(List<Object> arguments, int index) {
        List<?> values = argument(arguments, index, List.class);
        return values.stream().map(Integer.class::cast).toList();
    }

    private static String key(Class<?> facade,
                              String methodName,
                              String parameterTypes,
                              Class<?> resultType) {
        return facade.getName() + "#" + methodName + "(" + parameterTypes + "):"
                + (resultType == void.class ? "void" : resultType.getName());
    }
}

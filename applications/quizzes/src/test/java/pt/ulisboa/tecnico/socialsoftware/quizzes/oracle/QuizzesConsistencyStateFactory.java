package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.AggregateHandlesRegistry;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

final class QuizzesConsistencyStateFactory {

    private final ExecutionFunctionalities executions;
    private final UserFunctionalities users;
    private final TopicFunctionalities topics;
    private final QuestionFunctionalities questions;
    private final QuizFunctionalities quizzes;
    private final QuizAnswerFunctionalities answers;
    private final TournamentFunctionalities tournaments;
    private final QuizAnswerRepository quizAnswerRepository;

    QuizzesConsistencyStateFactory(
            ExecutionFunctionalities executions,
            UserFunctionalities users,
            TopicFunctionalities topics,
            QuestionFunctionalities questions,
            QuizFunctionalities quizzes,
            QuizAnswerFunctionalities answers,
            TournamentFunctionalities tournaments,
            QuizAnswerRepository quizAnswerRepository) {
        this.executions = executions;
        this.users = users;
        this.topics = topics;
        this.questions = questions;
        this.quizzes = quizzes;
        this.answers = answers;
        this.tournaments = tournaments;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    AggregateHandlesRegistry setup() {
        CourseExecutionDto mainExecution = createExecution("Neutral Course", "TECNICO", "NEUTRAL", "2026");
        CourseExecutionDto emptyExecution = createExecution("Disposable Course", "TECNICO", "DISPOSABLE", "2026");

        UserDto creator = createActiveUser("Creator", "creator");
        UserDto participant = createActiveUser("Participant", "participant");
        UserDto startStudent = createActiveUser("Starter", "starter");
        UserDto addStudent = createActiveUser("New Student", "new-student");
        UserDto deleteUser = createActiveUser("Disposable User", "disposable-user");
        UserDto inactiveUser = createUser("Inactive User", "inactive-user");

        executions.addStudent(mainExecution.getAggregateId(), creator.getAggregateId());
        executions.addStudent(mainExecution.getAggregateId(), participant.getAggregateId());
        executions.addStudent(mainExecution.getAggregateId(), startStudent.getAggregateId());

        TopicDto primaryTopic = createTopic(mainExecution, "Primary Topic");
        TopicDto secondaryTopic = createTopic(mainExecution, "Secondary Topic");
        TopicDto disposableTopic = createTopic(mainExecution, "Disposable Topic");

        QuestionDto primaryQuestion = createQuestion(mainExecution, List.of(primaryTopic), "Primary question");
        QuestionDto secondaryQuestion = createQuestion(mainExecution, List.of(secondaryTopic), "Secondary question");
        QuestionDto disposableQuestion = createQuestion(mainExecution, List.of(disposableTopic), "Disposable question");

        QuizDto quiz = createQuiz(mainExecution, List.of(primaryQuestion, secondaryQuestion), "Neutral quiz");
        answers.startQuiz(quiz.getAggregateId(), mainExecution.getAggregateId(), participant.getAggregateId());
        Integer quizAnswerId = quizAnswerRepository
                .findQuizAnswerIdByQuizIdAndUserIdForSaga(quiz.getAggregateId(), participant.getAggregateId())
                .orElseThrow();

        TournamentDto openTournament = createTournament(mainExecution, creator, primaryTopic, "open");
        tournaments.addParticipant(openTournament.getAggregateId(), mainExecution.getAggregateId(),
                participant.getAggregateId());
        TournamentDto cancellableTournament = createTournament(mainExecution, creator, primaryTopic, "cancellable");
        TournamentDto removableTournament = createTournament(mainExecution, creator, primaryTopic, "removable");
        tournaments.cancelTournament(removableTournament.getAggregateId());

        return new AggregateHandlesRegistry()
                .register("mainCourse", mainExecution.getCourseAggregateId())
                .register("mainExecution", mainExecution.getAggregateId())
                .register("emptyCourse", emptyExecution.getCourseAggregateId())
                .register("emptyExecution", emptyExecution.getAggregateId())
                .register("creator", creator.getAggregateId())
                .register("participant", participant.getAggregateId())
                .register("startStudent", startStudent.getAggregateId())
                .register("addStudent", addStudent.getAggregateId())
                .register("deleteUser", deleteUser.getAggregateId())
                .register("inactiveUser", inactiveUser.getAggregateId())
                .register("primaryTopic", primaryTopic.getAggregateId())
                .register("secondaryTopic", secondaryTopic.getAggregateId())
                .register("disposableTopic", disposableTopic.getAggregateId())
                .register("primaryQuestion", primaryQuestion.getAggregateId())
                .register("secondaryQuestion", secondaryQuestion.getAggregateId())
                .register("disposableQuestion", disposableQuestion.getAggregateId())
                .register("quiz", quiz.getAggregateId())
                .register("quizAnswer", quizAnswerId)
                .register("openTournament", openTournament.getAggregateId())
                .register("openTournamentQuiz", openTournament.getQuiz().getAggregateId())
                .register("cancellableTournament", cancellableTournament.getAggregateId())
                .register("cancellableTournamentQuiz", cancellableTournament.getQuiz().getAggregateId())
                .register("removableTournament", removableTournament.getAggregateId())
                .register("removableTournamentQuiz", removableTournament.getQuiz().getAggregateId());
    }

    private CourseExecutionDto createExecution(String name, String type, String acronym, String term) {
        CourseExecutionDto dto = new CourseExecutionDto();
        dto.setName(name);
        dto.setType(type);
        dto.setAcronym(acronym);
        dto.setAcademicTerm(term);
        dto.setEndDate(DateHandler.toISOString(DateHandler.now().plusDays(2)));
        return Objects.requireNonNull(executions.createCourseExecution(dto));
    }

    private UserDto createActiveUser(String name, String username) {
        UserDto user = createUser(name, username);
        users.activateUser(user.getAggregateId());
        return user;
    }

    private UserDto createUser(String name, String username) {
        UserDto dto = new UserDto();
        dto.setName(name);
        dto.setUsername(username);
        dto.setRole("STUDENT");
        return Objects.requireNonNull(users.createUser(dto));
    }

    private TopicDto createTopic(CourseExecutionDto execution, String name) {
        TopicDto dto = new TopicDto();
        dto.setName(name);
        return Objects.requireNonNull(topics.createTopic(execution.getCourseAggregateId(), dto));
    }

    private QuestionDto createQuestion(CourseExecutionDto execution, List<TopicDto> topicDtos, String title) {
        QuestionDto dto = questionInput(title, topicDtos);
        return Objects.requireNonNull(questions.createQuestion(execution.getCourseAggregateId(), dto));
    }

    static QuestionDto questionInput(String title, List<TopicDto> topicDtos) {
        QuestionDto dto = new QuestionDto();
        dto.setTitle(title);
        dto.setContent(title + " content");
        dto.setTopicDto(new HashSet<>(topicDtos));

        OptionDto correct = new OptionDto();
        correct.setSequence(1);
        correct.setCorrect(true);
        correct.setContent("Correct");
        OptionDto wrong = new OptionDto();
        wrong.setSequence(2);
        wrong.setCorrect(false);
        wrong.setContent("Wrong");
        dto.setOptionDtos(List.of(correct, wrong));
        return dto;
    }

    private QuizDto createQuiz(CourseExecutionDto execution, List<QuestionDto> questionDtos, String title) {
        try {
            return Objects.requireNonNull(quizzes.createQuiz(execution.getAggregateId(), quizInput(title, questionDtos)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not create neutral quiz state", e);
        }
    }

    static QuizDto quizInput(String title, List<QuestionDto> questionDtos) {
        LocalDateTime now = DateHandler.now();
        QuizDto dto = new QuizDto();
        dto.setTitle(title);
        dto.setAvailableDate(DateHandler.toISOString(now.plusMinutes(5)));
        dto.setConclusionDate(DateHandler.toISOString(now.plusHours(1)));
        dto.setResultsDate(DateHandler.toISOString(now.plusHours(2)));
        dto.setQuestionDtos(questionDtos);
        return dto;
    }

    private TournamentDto createTournament(
            CourseExecutionDto execution, UserDto creator, TopicDto topic, String suffix) {
        LocalDateTime now = DateHandler.now();
        TournamentDto dto = new TournamentDto();
        dto.setStartTime(DateHandler.toISOString(now.plusMinutes(10)));
        dto.setEndTime(DateHandler.toISOString(now.plusHours(1)));
        dto.setNumberOfQuestions(1);
        return Objects.requireNonNull(tournaments.createTournament(
                creator.getAggregateId(), execution.getAggregateId(), List.of(topic.getAggregateId()), dto), suffix);
    }
}

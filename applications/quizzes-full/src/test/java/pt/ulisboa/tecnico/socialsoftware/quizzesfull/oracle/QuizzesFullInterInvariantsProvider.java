package pt.ulisboa.tecnico.socialsoftware.quizzesfull.oracle;

import static pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState.DELETED;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariant;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantViolation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantsProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserRepository;

@Component
@Profile("oracle")
public class QuizzesFullInterInvariantsProvider implements InterInvariantsProvider {

    private final ExecutionRepository executionRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final TopicRepository topicRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public QuizzesFullInterInvariantsProvider(
            ExecutionRepository executionRepository,
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            QuizAnswerRepository quizAnswerRepository,
            TopicRepository topicRepository,
            TournamentRepository tournamentRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager) {
        this.executionRepository = executionRepository;
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.topicRepository = topicRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setReadOnly(true);
    }

    @Override
    public Set<InterInvariant> getInterInvariants() {
        return Set.of(
                invariant("REMOVE_NO_STUDENTS", this::removeNoStudents),
                invariant("NO_DUPLICATE_COURSE_EXECUTION", this::noDuplicateCourseExecution),
                invariant("INACTIVE_USER", this::enrolledUsersAreActive),
                invariant("STUDENT_ALREADY_ENROLLED", this::studentsAreUnique),
                invariant("USER_EXISTS_EXECUTION", this::executionUsersExist),
                invariant("TOPIC_BELONGS_TO_QUESTION_COURSE", this::questionTopicsBelongToCourse),
                invariant("TOPICS_EXIST_QUESTION", this::questionTopicsExist),
                invariant("QUESTION_EXISTS_QUIZ", this::quizQuestionsExist),
                invariant("COURSE_EXECUTION_EXISTS_QUIZ", this::quizExecutionsExist),
                invariant("UNIQUE_QUIZ_ANSWER_PER_STUDENT", this::quizAnswersAreUnique),
                invariant("QUESTION_ALREADY_ANSWERED", this::answeredQuestionsAreUnique),
                invariant("COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION", this::quizAnswerExecutionMatchesQuiz),
                invariant("USER_EXISTS_QUIZ_ANSWER", this::quizAnswerUsersExistAndRemainEnrolled),
                invariant("QUIZ_EXISTS_QUIZ_ANSWER", this::quizAnswerQuizzesExist),
                invariant("COURSE_EXECUTION_EXISTS_QUIZ_ANSWER", this::quizAnswerExecutionsExist),
                invariant("CREATOR_IS_NOT_ANONYMOUS_TOURNAMENT", this::tournamentCreatorsAreNotAnonymous),
                invariant("CREATOR_COURSE_EXECUTION_TOURNAMENT", this::tournamentCreatorsAreEnrolled),
                invariant("PARTICIPANT_COURSE_EXECUTION_TOURNAMENT", this::tournamentParticipantsAreEnrolled),
                invariant("TOPIC_COURSE_EXECUTION_TOURNAMENT", this::tournamentTopicsBelongToExecutionCourse),
                invariant("QUIZ_COURSE_EXECUTION_CONSISTENCY_TOURNAMENT", this::tournamentQuizMatchesExecution),
                invariant("TOURNAMENT_QUIZ_TIME_CONSISTENCY", this::tournamentTimesMatchQuiz),
                invariant("NUMBER_OF_QUESTIONS_QUIZ_TOPICS_TOURNAMENT", this::tournamentQuestionSelectionIsConsistent),
                invariant("CREATOR_PARTICIPANT_EXISTS_TOURNAMENT", this::tournamentUsersExist),
                invariant("TOPIC_EXISTS_TOURNAMENT", this::tournamentTopicsExist),
                invariant("QUIZ_EXISTS_TOURNAMENT", this::tournamentQuizzesExist),
                invariant("COURSE_EXECUTION_EXISTS_TOURNAMENT", this::tournamentExecutionsExist),
                invariant("QUIZ_ANSWER_EXISTS_TOURNAMENT", this::tournamentQuizAnswerStatisticsMatch));
    }

    private InterInvariant invariant(String name, Predicate<Snapshot> predicate) {
        Supplier<Set<InterInvariantViolation>> supplier = () -> {
            Boolean valid = transactionTemplate.execute(status -> predicate.test(snapshot()));
            if (Boolean.TRUE.equals(valid)) {
                return Set.of();
            }
            return Set.of(new InterInvariantViolation(name + " is violated by latest aggregate state"));
        };
        return new InterInvariant(name, supplier);
    }

    private Snapshot snapshot() {
        return new Snapshot(
                latest(executionRepository.findAll()),
                latest(questionRepository.findAll()),
                latest(quizRepository.findAll()),
                latest(quizAnswerRepository.findAll()),
                latest(topicRepository.findAll()),
                latest(tournamentRepository.findAll()),
                latest(userRepository.findAll()));
    }

    private <T extends Aggregate> Map<Integer, T> latest(Collection<T> aggregates) {
        return aggregates.stream().collect(Collectors.toMap(
                Aggregate::getAggregateId,
                aggregate -> aggregate,
                (left, right) -> Comparator.nullsFirst(Long::compareTo)
                        .compare(left.getVersion(), right.getVersion()) >= 0 ? left : right));
    }

    private boolean removeNoStudents(Snapshot state) {
        return state.executions.values().stream()
                .filter(execution -> execution.getState() == DELETED)
                .allMatch(execution -> execution.getStudents().isEmpty());
    }

    private boolean noDuplicateCourseExecution(Snapshot state) {
        Set<String> identities = new HashSet<>();
        return active(state.executions).stream()
                .allMatch(execution -> identities.add(execution.getAcronym() + "\u0000" + execution.getAcademicTerm()));
    }

    private boolean enrolledUsersAreActive(Snapshot state) {
        return active(state.executions).stream()
                .flatMap(execution -> execution.getStudents().stream())
                .allMatch(student -> isActive(state.users.get(student.getUserAggregateId())));
    }

    private boolean studentsAreUnique(Snapshot state) {
        return active(state.executions).stream().allMatch(execution -> {
            Set<Integer> userIds = new HashSet<>();
            return execution.getStudents().stream()
                    .allMatch(student -> userIds.add(student.getUserAggregateId()));
        });
    }

    private boolean executionUsersExist(Snapshot state) {
        return active(state.executions).stream()
                .flatMap(execution -> execution.getStudents().stream())
                .allMatch(student -> exists(state.users.get(student.getUserAggregateId())));
    }

    private boolean questionTopicsBelongToCourse(Snapshot state) {
        return active(state.questions).stream().allMatch(question -> question.getTopics().stream()
                .allMatch(topic -> Objects.equals(
                        topic.getCourseAggregateId(), question.getQuestionCourse().getCourseAggregateId())));
    }

    private boolean questionTopicsExist(Snapshot state) {
        return active(state.questions).stream().flatMap(question -> question.getTopics().stream())
                .allMatch(topic -> exists(state.topics.get(topic.getTopicAggregateId())));
    }

    private boolean quizQuestionsExist(Snapshot state) {
        return active(state.quizzes).stream().flatMap(quiz -> quiz.getQuestions().stream())
                .allMatch(question -> exists(state.questions.get(question.getQuestionAggregateId())));
    }

    private boolean quizExecutionsExist(Snapshot state) {
        return active(state.quizzes).stream()
                .allMatch(quiz -> exists(state.executions.get(quiz.getQuizExecution().getExecutionAggregateId())));
    }

    private boolean quizAnswersAreUnique(Snapshot state) {
        Set<String> identities = new HashSet<>();
        return active(state.quizAnswers).stream()
                .allMatch(answer -> identities.add(answer.getQuizAggregateId() + "\u0000" + answer.getUserAggregateId()));
    }

    private boolean answeredQuestionsAreUnique(Snapshot state) {
        return active(state.quizAnswers).stream().allMatch(answer -> {
            Set<Integer> questionIds = new HashSet<>();
            return answer.getQuestionAnswers().stream()
                    .allMatch(questionAnswer -> questionIds.add(questionAnswer.getQuestionAggregateId()));
        });
    }

    private boolean quizAnswerExecutionMatchesQuiz(Snapshot state) {
        return active(state.quizAnswers).stream().allMatch(answer -> {
            Quiz quiz = state.quizzes.get(answer.getQuizAggregateId());
            return !exists(quiz) || Objects.equals(
                    answer.getExecutionAggregateId(), quiz.getQuizExecution().getExecutionAggregateId());
        });
    }

    private boolean quizAnswerUsersExistAndRemainEnrolled(Snapshot state) {
        return active(state.quizAnswers).stream().allMatch(answer -> {
            User user = state.users.get(answer.getUserAggregateId());
            Execution execution = state.executions.get(answer.getExecutionAggregateId());
            return exists(user) && exists(execution) && execution.getStudents().stream()
                    .anyMatch(student -> Objects.equals(student.getUserAggregateId(), answer.getUserAggregateId()));
        });
    }

    private boolean quizAnswerQuizzesExist(Snapshot state) {
        return active(state.quizAnswers).stream()
                .allMatch(answer -> exists(state.quizzes.get(answer.getQuizAggregateId())));
    }

    private boolean quizAnswerExecutionsExist(Snapshot state) {
        return active(state.quizAnswers).stream()
                .allMatch(answer -> exists(state.executions.get(answer.getExecutionAggregateId())));
    }

    private boolean tournamentCreatorsAreNotAnonymous(Snapshot state) {
        return active(state.tournaments).stream()
                .allMatch(tournament -> !"ANONYMOUS".equals(tournament.getCreatorName())
                        && !"ANONYMOUS".equals(tournament.getCreatorUsername()));
    }

    private boolean tournamentCreatorsAreEnrolled(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Execution execution = state.executions.get(tournament.getExecutionAggregateId());
            return exists(execution) && execution.getStudents().stream().anyMatch(student ->
                    Objects.equals(student.getUserAggregateId(), tournament.getCreatorAggregateId()));
        });
    }

    private boolean tournamentParticipantsAreEnrolled(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Execution execution = state.executions.get(tournament.getExecutionAggregateId());
            if (!exists(execution)) {
                return false;
            }
            Set<Integer> enrolled = execution.getStudents().stream()
                    .map(student -> student.getUserAggregateId()).collect(Collectors.toSet());
            return tournament.getParticipants().stream()
                    .allMatch(participant -> enrolled.contains(participant.getParticipantAggregateId()));
        });
    }

    private boolean tournamentTopicsBelongToExecutionCourse(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Execution execution = state.executions.get(tournament.getExecutionAggregateId());
            if (!exists(execution)) {
                return false;
            }
            Integer courseId = execution.getExecutionCourse().getCourseAggregateId();
            return tournament.getTopics().stream().allMatch(topic -> {
                Topic referenced = state.topics.get(topic.getTopicAggregateId());
                return exists(referenced)
                        && Objects.equals(referenced.getTopicCourse().getCourseAggregateId(), courseId);
            });
        });
    }

    private boolean tournamentQuizMatchesExecution(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Quiz quiz = state.quizzes.get(tournament.getQuizAggregateId());
            return exists(quiz) && Objects.equals(
                    tournament.getExecutionAggregateId(), quiz.getQuizExecution().getExecutionAggregateId());
        });
    }

    private boolean tournamentTimesMatchQuiz(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Quiz quiz = state.quizzes.get(tournament.getQuizAggregateId());
            return exists(quiz)
                    && Objects.equals(tournament.getStartTime(), quiz.getAvailableDate())
                    && Objects.equals(tournament.getEndTime(), quiz.getConclusionDate());
        });
    }

    private boolean tournamentQuestionSelectionIsConsistent(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Quiz quiz = state.quizzes.get(tournament.getQuizAggregateId());
            if (!exists(quiz) || !Objects.equals(tournament.getNumberOfQuestions(), quiz.getQuestions().size())) {
                return false;
            }
            Set<Integer> tournamentTopicIds = tournament.getTopics().stream()
                    .map(topic -> topic.getTopicAggregateId()).collect(Collectors.toSet());
            for (var quizQuestion : quiz.getQuestions()) {
                Question question = state.questions.get(quizQuestion.getQuestionAggregateId());
                if (!exists(question) || question.getTopics().stream()
                        .anyMatch(topic -> !tournamentTopicIds.contains(topic.getTopicAggregateId()))) {
                    return false;
                }
            }
            return true;
        });
    }

    private boolean tournamentUsersExist(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            User creator = state.users.get(tournament.getCreatorAggregateId());
            if (!usableTournamentUser(creator)) {
                return false;
            }
            return tournament.getParticipants().stream().allMatch(participant ->
                    usableTournamentUser(state.users.get(participant.getParticipantAggregateId())));
        });
    }

    private boolean tournamentTopicsExist(Snapshot state) {
        return active(state.tournaments).stream().flatMap(tournament -> tournament.getTopics().stream())
                .allMatch(topic -> exists(state.topics.get(topic.getTopicAggregateId())));
    }

    private boolean tournamentQuizzesExist(Snapshot state) {
        return active(state.tournaments).stream()
                .allMatch(tournament -> exists(state.quizzes.get(tournament.getQuizAggregateId())));
    }

    private boolean tournamentExecutionsExist(Snapshot state) {
        return active(state.tournaments).stream()
                .allMatch(tournament -> exists(state.executions.get(tournament.getExecutionAggregateId())));
    }

    private boolean tournamentQuizAnswerStatisticsMatch(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> tournament.getParticipants().stream()
                .allMatch(participant -> {
                    var summary = participant.getQuizAnswer();
                    if (summary == null || summary.getQuizAnswerAggregateId() == null) {
                        return true;
                    }
                    QuizAnswer answer = state.quizAnswers.get(summary.getQuizAnswerAggregateId());
                    if (!exists(answer)) {
                        return false;
                    }
                    int answered = answer.getQuestionAnswers().size();
                    long correct = answer.getQuestionAnswers().stream()
                            .filter(questionAnswer -> Boolean.TRUE.equals(questionAnswer.getCorrect())).count();
                    return Objects.equals(summary.getNumberOfAnswered(), answered)
                            && Objects.equals(summary.getNumberOfCorrect(), Math.toIntExact(correct))
                            && Objects.equals(summary.getAnswered(), answered > 0);
                }));
    }

    private <T extends Aggregate> Collection<T> active(Map<Integer, T> aggregates) {
        return aggregates.values().stream().filter(this::isActive).toList();
    }

    private boolean isActive(Aggregate aggregate) {
        return aggregate != null && aggregate.getState() == ACTIVE;
    }

    private boolean exists(Aggregate aggregate) {
        return aggregate != null && aggregate.getState() != DELETED;
    }

    private boolean usableTournamentUser(User user) {
        return isActive(user) && !"ANONYMOUS".equals(user.getName()) && !"ANONYMOUS".equals(user.getUsername());
    }

    private record Snapshot(
            Map<Integer, Execution> executions,
            Map<Integer, Question> questions,
            Map<Integer, Quiz> quizzes,
            Map<Integer, QuizAnswer> quizAnswers,
            Map<Integer, Topic> topics,
            Map<Integer, Tournament> tournaments,
            Map<Integer, User> users) {
    }
}

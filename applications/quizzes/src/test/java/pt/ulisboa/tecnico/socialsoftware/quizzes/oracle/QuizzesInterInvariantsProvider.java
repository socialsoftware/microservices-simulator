package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository;

@Component
@Profile("oracle")
public class QuizzesInterInvariantsProvider implements InterInvariantsProvider {

    private final CourseRepository courseRepository;
    private final CourseExecutionRepository executionRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final TopicRepository topicRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public QuizzesInterInvariantsProvider(
            CourseRepository courseRepository,
            CourseExecutionRepository executionRepository,
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            QuizAnswerRepository quizAnswerRepository,
            TopicRepository topicRepository,
            TournamentRepository tournamentRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager) {
        this.courseRepository = courseRepository;
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
                invariant("CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT", this::courseContentHasAnExecution),
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
                invariant("ANSWER_BEFORE_START_TOURNAMENT", this::tournamentAnswersRespectStartTime),
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
                latest(courseRepository.findAll()),
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
                .filter(student -> student.isActive() && student.getState() == ACTIVE)
                .allMatch(student -> {
                    User user = state.users.get(student.getUserAggregateId());
                    return isActive(user) && Boolean.TRUE.equals(user.isActive());
                });
    }

    private boolean studentsAreUnique(Snapshot state) {
        return active(state.executions).stream().allMatch(execution -> {
            Set<Integer> userIds = new HashSet<>();
            return execution.getStudents().stream()
                    .filter(student -> student.isActive() && student.getState() == ACTIVE)
                    .allMatch(student -> userIds.add(student.getUserAggregateId()));
        });
    }

    private boolean executionUsersExist(Snapshot state) {
        return active(state.executions).stream()
                .flatMap(execution -> execution.getStudents().stream())
                .filter(student -> student.isActive() && student.getState() == ACTIVE)
                .allMatch(student -> exists(state.users.get(student.getUserAggregateId())));
    }

    private boolean courseContentHasAnExecution(Snapshot state) {
        return state.courses.values().stream()
                .allMatch(course -> course.getCourseExecutionCount() > 0 || course.getCourseQuestionCount() == 0);
    }

    private boolean questionTopicsBelongToCourse(Snapshot state) {
        return active(state.questions).stream().allMatch(question -> question.getQuestionTopics().stream()
                .filter(topic -> topic.getState() == ACTIVE)
                .allMatch(topic -> Objects.equals(
                        topic.getCourseAggregateId(), question.getQuestionCourse().getCourseAggregateId())));
    }

    private boolean questionTopicsExist(Snapshot state) {
        return active(state.questions).stream().flatMap(question -> question.getQuestionTopics().stream())
                .filter(topic -> topic.getState() == ACTIVE)
                .allMatch(topic -> exists(state.topics.get(topic.getTopicAggregateId())));
    }

    private boolean quizQuestionsExist(Snapshot state) {
        return active(state.quizzes).stream().flatMap(quiz -> quiz.getQuizQuestions().stream())
                .filter(question -> question.getState() == ACTIVE)
                .allMatch(question -> exists(state.questions.get(question.getQuestionAggregateId())));
    }

    private boolean quizExecutionsExist(Snapshot state) {
        return active(state.quizzes).stream()
                .allMatch(quiz -> exists(state.executions.get(
                        quiz.getQuizCourseExecution().getCourseExecutionAggregateId())));
    }

    private boolean quizAnswersAreUnique(Snapshot state) {
        Set<String> identities = new HashSet<>();
        return active(state.quizAnswers).stream().allMatch(answer -> identities.add(
                answer.getQuiz().getQuizAggregateId() + "\u0000" + answer.getStudent().getStudentAggregateId()));
    }

    private boolean answeredQuestionsAreUnique(Snapshot state) {
        return active(state.quizAnswers).stream().allMatch(answer -> {
            Set<Integer> questionIds = new HashSet<>();
            return answer.getQuestionAnswers().stream()
                    .filter(questionAnswer -> questionAnswer.getState() == ACTIVE)
                    .allMatch(questionAnswer -> questionIds.add(questionAnswer.getQuestionAggregateId()));
        });
    }

    private boolean quizAnswerExecutionMatchesQuiz(Snapshot state) {
        return active(state.quizAnswers).stream().allMatch(answer -> {
            Quiz quiz = state.quizzes.get(answer.getQuiz().getQuizAggregateId());
            return !exists(quiz) || Objects.equals(
                    answer.getAnswerCourseExecution().getCourseExecutionAggregateId(),
                    quiz.getQuizCourseExecution().getCourseExecutionAggregateId());
        });
    }

    private boolean quizAnswerUsersExistAndRemainEnrolled(Snapshot state) {
        return active(state.quizAnswers).stream().allMatch(answer -> {
            Integer userId = answer.getStudent().getStudentAggregateId();
            User user = state.users.get(userId);
            Execution execution = state.executions.get(
                    answer.getAnswerCourseExecution().getCourseExecutionAggregateId());
            return exists(user) && exists(execution) && execution.getStudents().stream()
                    .anyMatch(student -> Objects.equals(student.getUserAggregateId(), userId)
                            && student.isActive() && student.getState() == ACTIVE);
        });
    }

    private boolean quizAnswerQuizzesExist(Snapshot state) {
        return active(state.quizAnswers).stream()
                .allMatch(answer -> isActive(state.quizzes.get(answer.getQuiz().getQuizAggregateId())));
    }

    private boolean quizAnswerExecutionsExist(Snapshot state) {
        return active(state.quizAnswers).stream()
                .allMatch(answer -> exists(state.executions.get(
                        answer.getAnswerCourseExecution().getCourseExecutionAggregateId())));
    }

    private boolean tournamentAnswersRespectStartTime(Snapshot state) {
        return active(state.tournaments).stream().allMatch(Tournament::invariantAnswerBeforeStart);
    }

    private boolean tournamentCreatorsAreNotAnonymous(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament ->
                !"ANONYMOUS".equals(tournament.getTournamentCreator().getCreatorName())
                        && !"ANONYMOUS".equals(tournament.getTournamentCreator().getCreatorUsername()));
    }

    private boolean tournamentCreatorsAreEnrolled(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Execution execution = state.executions.get(
                    tournament.getTournamentCourseExecution().getCourseExecutionAggregateId());
            return exists(execution) && execution.getStudents().stream().anyMatch(student ->
                    Objects.equals(student.getUserAggregateId(),
                            tournament.getTournamentCreator().getCreatorAggregateId())
                            && student.isActive() && student.getState() == ACTIVE);
        });
    }

    private boolean tournamentParticipantsAreEnrolled(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Execution execution = state.executions.get(
                    tournament.getTournamentCourseExecution().getCourseExecutionAggregateId());
            if (!exists(execution)) {
                return false;
            }
            Set<Integer> enrolled = execution.getStudents().stream()
                    .filter(student -> student.isActive() && student.getState() == ACTIVE)
                    .map(student -> student.getUserAggregateId()).collect(Collectors.toSet());
            return tournament.getTournamentParticipants().stream()
                    .filter(participant -> participant.getState() == ACTIVE)
                    .allMatch(participant -> enrolled.contains(participant.getParticipantAggregateId()));
        });
    }

    private boolean tournamentTopicsBelongToExecutionCourse(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Execution execution = state.executions.get(
                    tournament.getTournamentCourseExecution().getCourseExecutionAggregateId());
            if (!exists(execution)) {
                return false;
            }
            Integer courseId = execution.getExecutionCourse().getCourseAggregateId();
            return tournament.getTournamentTopics().stream()
                    .filter(topic -> topic.getState() == ACTIVE)
                    .allMatch(topic -> {
                        Topic referenced = state.topics.get(topic.getTopicAggregateId());
                        return exists(referenced)
                                && Objects.equals(referenced.getTopicCourse().getCourseAggregateId(), courseId);
                    });
        });
    }

    private boolean tournamentQuizMatchesExecution(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Quiz quiz = state.quizzes.get(tournament.getTournamentQuiz().getQuizAggregateId());
            return isActive(quiz) && Objects.equals(
                    tournament.getTournamentCourseExecution().getCourseExecutionAggregateId(),
                    quiz.getQuizCourseExecution().getCourseExecutionAggregateId());
        });
    }

    private boolean tournamentTimesMatchQuiz(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Quiz quiz = state.quizzes.get(tournament.getTournamentQuiz().getQuizAggregateId());
            return isActive(quiz)
                    && Objects.equals(tournament.getStartTime(), quiz.getAvailableDate())
                    && Objects.equals(tournament.getEndTime(), quiz.getConclusionDate());
        });
    }

    private boolean tournamentQuestionSelectionIsConsistent(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            Quiz quiz = state.quizzes.get(tournament.getTournamentQuiz().getQuizAggregateId());
            if (!isActive(quiz)) {
                return false;
            }
            var activeQuizQuestions = quiz.getQuizQuestions().stream()
                    .filter(question -> question.getState() == ACTIVE).toList();
            if (!Objects.equals(tournament.getNumberOfQuestions(), activeQuizQuestions.size())) {
                return false;
            }
            Set<Integer> tournamentTopicIds = tournament.getTournamentTopics().stream()
                    .filter(topic -> topic.getState() == ACTIVE)
                    .map(topic -> topic.getTopicAggregateId()).collect(Collectors.toSet());
            for (var quizQuestion : activeQuizQuestions) {
                Question question = state.questions.get(quizQuestion.getQuestionAggregateId());
                if (!exists(question) || question.getQuestionTopics().stream()
                        .filter(topic -> topic.getState() == ACTIVE)
                        .anyMatch(topic -> !tournamentTopicIds.contains(topic.getTopicAggregateId()))) {
                    return false;
                }
            }
            return true;
        });
    }

    private boolean tournamentUsersExist(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament -> {
            User creator = state.users.get(tournament.getTournamentCreator().getCreatorAggregateId());
            if (!usableTournamentUser(creator)) {
                return false;
            }
            return tournament.getTournamentParticipants().stream()
                    .filter(participant -> participant.getState() == ACTIVE)
                    .allMatch(participant -> usableTournamentUser(
                            state.users.get(participant.getParticipantAggregateId())));
        });
    }

    private boolean tournamentTopicsExist(Snapshot state) {
        return active(state.tournaments).stream().flatMap(tournament -> tournament.getTournamentTopics().stream())
                .filter(topic -> topic.getState() == ACTIVE)
                .allMatch(topic -> exists(state.topics.get(topic.getTopicAggregateId())));
    }

    private boolean tournamentQuizzesExist(Snapshot state) {
        return active(state.tournaments).stream()
                .allMatch(tournament -> isActive(state.quizzes.get(
                        tournament.getTournamentQuiz().getQuizAggregateId())));
    }

    private boolean tournamentExecutionsExist(Snapshot state) {
        return active(state.tournaments).stream()
                .allMatch(tournament -> exists(state.executions.get(
                        tournament.getTournamentCourseExecution().getCourseExecutionAggregateId())));
    }

    private boolean tournamentQuizAnswerStatisticsMatch(Snapshot state) {
        return active(state.tournaments).stream().allMatch(tournament ->
                tournament.getTournamentParticipants().stream()
                        .filter(participant -> participant.getState() == ACTIVE)
                        .allMatch(participant -> {
                            var summary = participant.getParticipantAnswer();
                            if (summary == null || summary.getQuizAnswerAggregateId() == null) {
                                return true;
                            }
                            QuizAnswer answer = state.quizAnswers.get(summary.getQuizAnswerAggregateId());
                            if (!exists(answer)) {
                                return false;
                            }
                            var activeAnswers = answer.getQuestionAnswers().stream()
                                    .filter(questionAnswer -> questionAnswer.getState() == ACTIVE).toList();
                            int answered = activeAnswers.size();
                            long correct = activeAnswers.stream().filter(questionAnswer -> questionAnswer.isCorrect()).count();
                            return Objects.equals(summary.getNumberOfAnswered(), answered)
                                    && Objects.equals(summary.getNumberOfCorrect(), Math.toIntExact(correct))
                                    && summary.isAnswered() == (answered > 0);
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
            Map<Integer, Course> courses,
            Map<Integer, Execution> executions,
            Map<Integer, Question> questions,
            Map<Integer, Quiz> quizzes,
            Map<Integer, QuizAnswer> quizAnswers,
            Map<Integer, Topic> topics,
            Map<Integer, Tournament> tournaments,
            Map<Integer, User> users) {
    }
}

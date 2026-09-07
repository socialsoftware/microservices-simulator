package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.diagnostics

import org.springframework.boot.test.context.runner.ApplicationContextRunner
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.PersistentStateObserver
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseAdapter
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.quiz.GetQuizByIdCommand
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.GetTournamentByIdCommand
import pt.ulisboa.tecnico.socialsoftware.quizzes.diagnostics.QuizzesSagaReadResponseAdapters
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizCourseExecution
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.SagaQuiz
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.SagaTournament
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import spock.lang.Specification
import spock.lang.Unroll

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizType.GENERATED

class QuizzesSagaReadResponseAdaptersTest extends Specification {
    private final config = new QuizzesSagaReadResponseAdapters()

    def 'Quiz contract agrees with persistent identity and DTO constructor without borrowing course version'() {
        given:
        def course = new CourseExecutionDto(aggregateId: 41, version: 97L)
        def input = new QuizDto(availableDate: '2030-01-01T12:05:00Z',
                conclusionDate: '2030-01-01T13:00:00Z', resultsDate: '2030-01-01T13:00:00Z')
        def aggregate = new SagaQuiz(12, new QuizCourseExecution(course), [] as Set, input, GENERATED)
        aggregate.version = 9L
        def response = new QuizDto(aggregate)
        def adapter = config.quizReadResponseAdapter()
        def identity = new PersistentStateObserver(null).identityOf(aggregate)

        expect:
        adapter.commandType() == GetQuizByIdCommand
        adapter.responseType() == response.class
        adapter.aggregateType() == identity.aggregateType()
        adapter.runtimeType() == aggregate.class.name
        adapter.aggregateId(response) == identity.aggregateId()
        adapter.version(response) == aggregate.version
        adapter.version(response) != response.courseExecutionVersion
    }

    def 'Tournament outer contract preserves exact revision while nested Quiz remains versionless'() {
        given:
        def input = new TournamentDto(startTime: '2030-01-01T12:05:00Z',
                endTime: '2030-01-01T13:00:00Z', numberOfQuestions: 2)
        def course = new CourseExecutionDto(aggregateId: 41, version: 97L)
        def creator = new UserDto(aggregateId: 42, version: 98L, name: 'Fixture', username: 'fixture')
        def aggregate = new SagaTournament(13, input, creator, course, [] as Set,
                new QuizDto(aggregateId: 12, version: 9L))
        aggregate.version = 10L
        def response = new TournamentDto(aggregate)
        def adapter = config.tournamentReadResponseAdapter()
        def identity = new PersistentStateObserver(null).identityOf(aggregate)

        expect:
        adapter.commandType() == GetTournamentByIdCommand
        adapter.responseType() == response.class
        adapter.aggregateType() == identity.aggregateType()
        adapter.runtimeType() == aggregate.class.name
        adapter.aggregateId(response) == 13
        adapter.version(response) == 10L
        response.quiz.aggregateId == 12
        aggregate.tournamentQuiz.quizVersion == 9L
        response.quiz.version == null
    }

    def 'missing outer revision never borrows a present nested revision'() {
        given:
        def response = new TournamentDto(aggregateId: 13, version: null,
                quiz: new QuizDto(aggregateId: 12, version: 9L))

        expect:
        config.tournamentReadResponseAdapter().version(response) == null
        config.tournamentReadResponseAdapter().aggregateId(response) == 13
    }

    @Unroll
    def 'adapters are registered only for Saga local profiles: #profiles'() {
        expect:
        new ApplicationContextRunner().withUserConfiguration(QuizzesSagaReadResponseAdapters)
                .withPropertyValues("spring.profiles.active=${profiles}")
                .run { context ->
                    assert context.getBeansOfType(ReadResponseAdapter).size() == count
                }

        where:
        profiles       | count
        'sagas,local'  | 2
        'sagas,stream' | 0
        'tcc,local'    | 0
    }
}

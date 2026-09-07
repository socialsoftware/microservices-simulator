package pt.ulisboa.tecnico.socialsoftware.quizzes.diagnostics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseAdapter;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.SagaTournament;

/** Audited outer-response provenance for the two singular Saga/local lookup handlers. */
@Configuration(proxyBeanMethods = false)
@Profile("sagas & local")
public class QuizzesSagaReadResponseAdapters {
    @Bean
    public ReadResponseAdapter<GetQuizByIdCommand, QuizDto> quizReadResponseAdapter() {
        // QuizService.getQuizById -> SagasQuizFactory.createQuizDto -> QuizDto(Quiz).
        return new ReadResponseAdapter<>() {
            public Class<GetQuizByIdCommand> commandType() { return GetQuizByIdCommand.class; }
            public Class<QuizDto> responseType() { return QuizDto.class; }
            public String contractId() { return "quizzes.saga-local.quiz-by-id.outer"; }
            public String contractVersion() { return "1"; }
            public String aggregateType() { return "SagaQuiz"; }
            public String runtimeType() { return SagaQuiz.class.getName(); }
            public Integer aggregateId(QuizDto response) { return response.getAggregateId(); }
            public Long version(QuizDto response) { return response.getVersion(); }
        };
    }

    @Bean
    public ReadResponseAdapter<GetTournamentByIdCommand, TournamentDto> tournamentReadResponseAdapter() {
        // TournamentService.getTournamentById -> SagasTournamentFactory -> TournamentDto(Tournament).
        // TournamentDto.quiz is a nested reference and is deliberately outside this contract.
        return new ReadResponseAdapter<>() {
            public Class<GetTournamentByIdCommand> commandType() { return GetTournamentByIdCommand.class; }
            public Class<TournamentDto> responseType() { return TournamentDto.class; }
            public String contractId() { return "quizzes.saga-local.tournament-by-id.outer"; }
            public String contractVersion() { return "1"; }
            public String aggregateType() { return "SagaTournament"; }
            public String runtimeType() { return SagaTournament.class.getName(); }
            public Integer aggregateId(TournamentDto response) { return response.getAggregateId(); }
            public Long version(TournamentDto response) { return response.getVersion(); }
        };
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service;

import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;

public interface TournamentFunctionalitiesInterface {
    TournamentDto createTournament(Integer userId, Integer executionId, List<Integer> topicsId,
                                          TournamentDto tournamentDto) throws Exception;
    void addParticipant(Integer tournamentAggregateId, Integer userAggregateId) throws Exception;
    void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) throws Exception;
    List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId);
    List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId);
    List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId);
    void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) throws Exception;
    QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) throws Exception;
    void cancelTournament(Integer tournamentAggregateId) throws Exception;
    void removeTournament(Integer tournamentAggregateId) throws Exception;
    TournamentDto findTournament(Integer tournamentAggregateId);
    void getTournamentAndUser(Integer tournamentAggregateId, Integer userAggregateId);

}

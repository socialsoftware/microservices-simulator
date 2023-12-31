package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.List;
import java.util.Set;

@RestController
public class TournamentController {
    private final Logger logger = LoggerFactory.getLogger(TournamentController.class);

    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    @PostMapping(value = "/executions/{executionId}/tournaments/create")
    public TournamentDto createTournament(@PathVariable int executionId, @RequestParam Integer userId, @RequestParam List<Integer> topicsId, @RequestBody TournamentDto tournamentDto) {
        return tournamentFunctionalities.createTournament(userId, executionId, topicsId, tournamentDto);
    }

    @PostMapping(value = "/tournaments/update")
    public void updateTournament(@RequestParam Set<Integer> topicsId, @RequestBody TournamentDto tournamentDto) {
        tournamentFunctionalities.updateTournament(tournamentDto, topicsId);
    }

    @PostMapping(value = "/tournaments/{tournamentAggregateId}/join")
    public void joinTournament(@PathVariable Integer tournamentAggregateId, @RequestParam Integer userAggregateId) {
        tournamentFunctionalities.addParticipant(tournamentAggregateId, userAggregateId);
    }

    @GetMapping(value = "/tournaments/{tournamentAggregateId}")
    public TournamentDto findTournament(@PathVariable Integer tournamentAggregateId) {
        return tournamentFunctionalities.findTournament(tournamentAggregateId);
    }

    @PostMapping(value = "/tournaments/{tournamentAggregateId}/solveQuiz")
    public QuizDto solveQuiz(@PathVariable Integer tournamentAggregateId, @RequestParam Integer userAggregateId) {
        return tournamentFunctionalities.solveQuiz(tournamentAggregateId, userAggregateId);
    }

    @GetMapping(value = "/executions/{executionAggregateId}/tournaments")
    public List<TournamentDto> getTournamentsForCourseExecution(@PathVariable Integer executionAggregateId) {
        return tournamentFunctionalities.getTournamentsForCourseExecution(executionAggregateId);
    }

    @GetMapping(value = "/executions/{executionAggregateId}/tournaments/opened")
    public List<TournamentDto> getOpenedTournamentsForCourseExecution(@PathVariable Integer executionAggregateId) {
        return tournamentFunctionalities.getOpenedTournamentsForCourseExecution(executionAggregateId);
    }

    @GetMapping(value = "/executions/{executionAggregateId}/tournaments/closed")
    public List<TournamentDto> getClosedTournamentsForCourseExecution(@PathVariable Integer executionAggregateId) {
        return tournamentFunctionalities.getClosedTournamentsForCourseExecution(executionAggregateId);
    }

    @PostMapping("/tournaments/{tournamentAggregate}/remove")
    public void removeTournament(@PathVariable Integer tournamentAggregate) {
        tournamentFunctionalities.removeTournament(tournamentAggregate);
    }

    @GetMapping(value = "/tournaments/{tournamentAggregateId}/user/{userAggregateId}")
    public void getTournamentAndUser(@PathVariable Integer tournamentAggregateId, @PathVariable Integer userAggregateId) {
        tournamentFunctionalities.getTournamentAndUser(tournamentAggregateId, userAggregateId);
    }
}

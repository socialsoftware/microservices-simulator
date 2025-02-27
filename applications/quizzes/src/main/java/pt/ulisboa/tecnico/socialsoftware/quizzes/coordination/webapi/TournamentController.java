package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.webapi;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;

@RestController
public class TournamentController {

    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    @PostMapping(value = "/executions/{executionId}/tournaments/create")
    public TournamentDto createTournament(@PathVariable int executionId, @RequestParam Integer userId, @RequestParam List<Integer> topicsId, @RequestBody TournamentDto tournamentDto) throws Exception {
        return tournamentFunctionalities.createTournament(userId, executionId, topicsId, tournamentDto);
    }

    @PostMapping(value = "/tournaments/update")
    public void updateTournament(@RequestParam Set<Integer> topicsId, @RequestBody TournamentDto tournamentDto) throws Exception {
        tournamentFunctionalities.updateTournament(tournamentDto, topicsId);
    }

    @PostMapping(value = "/tournaments/{tournamentAggregateId}/join")
    public void joinTournament(@PathVariable Integer tournamentAggregateId, @RequestParam Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        tournamentFunctionalities.addParticipant(tournamentAggregateId, executionAggregateId, userAggregateId);
    }

    @GetMapping(value = "/tournaments/{tournamentAggregateId}")
    public TournamentDto findTournament(@PathVariable Integer tournamentAggregateId) {
        return tournamentFunctionalities.findTournament(tournamentAggregateId);
    }

    @PostMapping(value = "/tournaments/{tournamentAggregateId}/solveQuiz")
    public QuizDto solveQuiz(@PathVariable Integer tournamentAggregateId, @RequestParam Integer userAggregateId) throws Exception {
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
    public void removeTournament(@PathVariable Integer tournamentAggregate) throws Exception {
        tournamentFunctionalities.removeTournament(tournamentAggregate);
    }

    @GetMapping(value = "/tournaments/{tournamentAggregateId}/user/{userAggregateId}")
    public void getTournamentAndUser(@PathVariable Integer tournamentAggregateId, @PathVariable Integer userAggregateId) {
        tournamentFunctionalities.getTournamentAndUser(tournamentAggregateId, userAggregateId);
    }
}

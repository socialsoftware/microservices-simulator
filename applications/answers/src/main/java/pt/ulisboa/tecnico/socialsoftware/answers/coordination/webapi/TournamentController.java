package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
public class TournamentController {
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    @PostMapping("/executions/{executionId}/tournaments/create")
    public TournamentDto createTournament(@PathVariable Integer executionId, @RequestParam Integer userId, @RequestParam String topicsId, @RequestBody TournamentDto tournamentDto) throws Exception {
        TournamentDto result = tournamentFunctionalities.createTournament(executionId, userId, topicsId, tournamentDto);
        return result;
    }

    @PostMapping("/tournaments/{tournamentAggregateId}/join")
    public void addParticipant(@PathVariable Integer tournamentAggregateId, @RequestParam Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        tournamentFunctionalities.addParticipant(tournamentAggregateId, executionAggregateId, userAggregateId);
    }

    @PostMapping("/tournaments/update")
    public void updateTournament(@RequestParam String topicsId, @RequestBody TournamentDto tournamentDto) throws Exception {
        tournamentFunctionalities.updateTournament(topicsId, tournamentDto);
    }

    @GetMapping("/tournaments/{tournamentAggregateId}")
    public TournamentDto findTournament(@PathVariable Integer tournamentAggregateId) {
        TournamentDto result = tournamentFunctionalities.findTournament(tournamentAggregateId);
        return result;
    }

    @PostMapping("/tournaments/{tournamentAggregateId}/solveQuiz")
    public String solveQuiz(@PathVariable Integer tournamentAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        String result = tournamentFunctionalities.solveQuiz(tournamentAggregateId, userAggregateId);
        return result;
    }

    @GetMapping("/executions/{executionAggregateId}/tournaments")
    public List<TournamentDto> getTournamentsForCourseExecution(@PathVariable Integer executionAggregateId) {
        List<TournamentDto> result = tournamentFunctionalities.getTournamentsForCourseExecution(executionAggregateId);
        return result;
    }

    @GetMapping("/executions/{executionAggregateId}/tournaments/opened")
    public List<TournamentDto> getOpenedTournamentsForCourseExecution(@PathVariable Integer executionAggregateId) {
        List<TournamentDto> result = tournamentFunctionalities.getOpenedTournamentsForCourseExecution(executionAggregateId);
        return result;
    }

    @GetMapping("/executions/{executionAggregateId}/tournaments/closed")
    public List<TournamentDto> getClosedTournamentsForCourseExecution(@PathVariable Integer executionAggregateId) {
        List<TournamentDto> result = tournamentFunctionalities.getClosedTournamentsForCourseExecution(executionAggregateId);
        return result;
    }

    @PostMapping("/tournaments/{tournamentAggregate}/remove")
    public void removeTournament(@PathVariable Integer tournamentAggregate) throws Exception {
        tournamentFunctionalities.removeTournament(tournamentAggregate);
    }

    @GetMapping("/tournaments/{tournamentAggregateId}/user/{userAggregateId}")
    public void getTournamentAndUser(@PathVariable Integer tournamentAggregateId, @PathVariable Integer userAggregateId) {
        tournamentFunctionalities.getTournamentAndUser(tournamentAggregateId, userAggregateId);
    }
}

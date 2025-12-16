package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.TournamentFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

@RestController
public class TournamentController {
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    @PostMapping("/executions/{executionId}/tournaments/create")
    public TournamentDto createTournament(@PathVariable Integer executionId, @RequestParam Integer userId, @RequestParam String topicsId, @RequestBody TournamentDto tournamentDto) throws Exception {
        return tournamentFunctionalities.createTournament(executionId, userId, topicsId, tournamentDto);
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
        return tournamentFunctionalities.findTournament(tournamentAggregateId);
    }

    @PostMapping("/tournaments/{tournamentAggregateId}/solveQuiz")
    public String solveQuiz(@PathVariable Integer tournamentAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        return tournamentFunctionalities.solveQuiz(tournamentAggregateId, userAggregateId);
    }

    @GetMapping("/executions/{executionAggregateId}/tournaments")
    public List<TournamentDto> getTournamentsForExecution(@PathVariable Integer executionAggregateId) {
        return tournamentFunctionalities.getTournamentsForExecution(executionAggregateId);
    }

    @GetMapping("/executions/{executionAggregateId}/tournaments/opened")
    public List<TournamentDto> getOpenedTournamentsForExecution(@PathVariable Integer executionAggregateId) {
        return tournamentFunctionalities.getOpenedTournamentsForExecution(executionAggregateId);
    }

    @GetMapping("/executions/{executionAggregateId}/tournaments/closed")
    public List<TournamentDto> getClosedTournamentsForExecution(@PathVariable Integer executionAggregateId) {
        return tournamentFunctionalities.getClosedTournamentsForExecution(executionAggregateId);
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

package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.TournamentFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTournamentRequestDto;

@RestController
public class TournamentController {
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    @PostMapping("/tournaments/create")
    public TournamentDto createTournament(@RequestBody CreateTournamentRequestDto createRequest) {
        return tournamentFunctionalities.createTournament(createRequest);
    }

    @GetMapping("/tournaments/{tournamentAggregateId}")
    public TournamentDto getTournamentById(@PathVariable Integer tournamentAggregateId) {
        return tournamentFunctionalities.getTournamentById(tournamentAggregateId);
    }

    @PutMapping("/tournaments")
    public TournamentDto updateTournament(@RequestBody TournamentDto tournamentDto) {
        return tournamentFunctionalities.updateTournament(tournamentDto);
    }

    @DeleteMapping("/tournaments/{tournamentAggregateId}")
    public void deleteTournament(@PathVariable Integer tournamentAggregateId) {
        tournamentFunctionalities.deleteTournament(tournamentAggregateId);
    }

    @GetMapping("/tournaments")
    public List<TournamentDto> searchTournaments(@RequestParam(required = false) Boolean cancelled, @RequestParam(required = false) Integer creatorAggregateId, @RequestParam(required = false) Integer quizAggregateId) {
        return tournamentFunctionalities.searchTournaments(cancelled, creatorAggregateId, quizAggregateId);
    }
}

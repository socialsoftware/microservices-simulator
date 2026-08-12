package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto;

@RestController
public class TournamentController {
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    @PostMapping("/tournaments/create")
    @ResponseStatus(HttpStatus.CREATED)
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTournament(@PathVariable Integer tournamentAggregateId) {
        tournamentFunctionalities.deleteTournament(tournamentAggregateId);
    }

    @GetMapping("/tournaments")
    public List<TournamentDto> getAllTournaments() {
        return tournamentFunctionalities.getAllTournaments();
    }

    @PostMapping("/tournaments/{tournamentId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentParticipantDto addTournamentParticipant(@PathVariable Integer tournamentId, @RequestParam Integer participantAggregateId, @RequestBody TournamentParticipantDto participantDto) {
        return tournamentFunctionalities.addTournamentParticipant(tournamentId, participantAggregateId, participantDto);
    }

    @PostMapping("/tournaments/{tournamentId}/participants/batch")
    public List<TournamentParticipantDto> addTournamentParticipants(@PathVariable Integer tournamentId, @RequestBody List<TournamentParticipantDto> participantDtos) {
        return tournamentFunctionalities.addTournamentParticipants(tournamentId, participantDtos);
    }

    @GetMapping("/tournaments/{tournamentId}/participants/{participantAggregateId}")
    public TournamentParticipantDto getTournamentParticipant(@PathVariable Integer tournamentId, @PathVariable Integer participantAggregateId) {
        return tournamentFunctionalities.getTournamentParticipant(tournamentId, participantAggregateId);
    }

    @PutMapping("/tournaments/{tournamentId}/participants/{participantAggregateId}")
    public TournamentParticipantDto updateTournamentParticipant(@PathVariable Integer tournamentId, @PathVariable Integer participantAggregateId, @RequestBody TournamentParticipantDto participantDto) {
        return tournamentFunctionalities.updateTournamentParticipant(tournamentId, participantAggregateId, participantDto);
    }

    @DeleteMapping("/tournaments/{tournamentId}/participants/{participantAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTournamentParticipant(@PathVariable Integer tournamentId, @PathVariable Integer participantAggregateId) {
        tournamentFunctionalities.removeTournamentParticipant(tournamentId, participantAggregateId);
    }

    @PostMapping("/tournaments/{tournamentId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentTopicDto addTournamentTopic(@PathVariable Integer tournamentId, @RequestParam Integer topicAggregateId, @RequestBody TournamentTopicDto topicDto) {
        return tournamentFunctionalities.addTournamentTopic(tournamentId, topicAggregateId, topicDto);
    }

    @PostMapping("/tournaments/{tournamentId}/topics/batch")
    public List<TournamentTopicDto> addTournamentTopics(@PathVariable Integer tournamentId, @RequestBody List<TournamentTopicDto> topicDtos) {
        return tournamentFunctionalities.addTournamentTopics(tournamentId, topicDtos);
    }

    @GetMapping("/tournaments/{tournamentId}/topics/{topicAggregateId}")
    public TournamentTopicDto getTournamentTopic(@PathVariable Integer tournamentId, @PathVariable Integer topicAggregateId) {
        return tournamentFunctionalities.getTournamentTopic(tournamentId, topicAggregateId);
    }

    @PutMapping("/tournaments/{tournamentId}/topics/{topicAggregateId}")
    public TournamentTopicDto updateTournamentTopic(@PathVariable Integer tournamentId, @PathVariable Integer topicAggregateId, @RequestBody TournamentTopicDto topicDto) {
        return tournamentFunctionalities.updateTournamentTopic(tournamentId, topicAggregateId, topicDto);
    }

    @DeleteMapping("/tournaments/{tournamentId}/topics/{topicAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTournamentTopic(@PathVariable Integer tournamentId, @PathVariable Integer topicAggregateId) {
        tournamentFunctionalities.removeTournamentTopic(tournamentId, topicAggregateId);
    }
}

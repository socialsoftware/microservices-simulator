package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournament")
public class TournamentController {

@Autowired
private TournamentService tournamentService;

@GetMapping(value = "/executions/{executionId}/tournaments/create")
public ResponseEntity<Object> createTournament(
        @PathVariable Integer executionId,
        @RequestParam Integer userId,
        @RequestParam String topicsId,
        @RequestBody Object tournamentDto
        ) throws Exception {
        Object result = tournamentService.createTournament(executionId,
        userId,
        topicsId,
        tournamentDto);
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/tournaments/{tournamentAggregateId}/join")
public ResponseEntity<String> joinTournament(
        @PathVariable Integer tournamentAggregateId,
        @RequestParam Integer executionAggregateId,
        @RequestParam Integer userAggregateId
        ) throws Exception {
        String result = tournamentService.joinTournament(tournamentAggregateId,
        executionAggregateId,
        userAggregateId);
        return ResponseEntity.ok(result);
        }

        }
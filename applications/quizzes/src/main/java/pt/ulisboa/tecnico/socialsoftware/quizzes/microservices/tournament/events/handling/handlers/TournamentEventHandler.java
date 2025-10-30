package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class TournamentEventHandler extends EventHandler {
    private TournamentRepository tournamentRepository;
    protected TournamentEventProcessing tournamentEventProcessing;

    public TournamentEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentEventProcessing = tournamentEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return tournamentRepository.findAll().stream().map(Tournament::getAggregateId).collect(Collectors.toSet());
    }

}

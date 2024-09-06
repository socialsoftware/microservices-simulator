package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentRepository;

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

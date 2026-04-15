package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;

public abstract class TournamentEventHandler extends EventHandler {
    protected TournamentEventProcessing tournamentEventProcessing;

    public TournamentEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        super(tournamentRepository);
        this.tournamentEventProcessing = tournamentEventProcessing;
    }

}

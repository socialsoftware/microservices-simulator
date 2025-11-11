package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipantQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;


@Service
@Transactional
public class TournamentService {
    private static final Logger logger = LoggerFactory.getLogger(TournamentService.class);

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentFactory tournamentFactory;

    public TournamentService() {}

    // CRUD Operations
    public TournamentDto createTournament(LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled, TournamentCreator tournamentCreator, Set<TournamentParticipant> tournamentParticipants, TournamentExecution tournamentExecution, Set<TournamentTopic> tournamentTopics, TournamentQuiz tournamentQuiz) {
        try {
            Tournament tournament = new Tournament(startTime, endTime, numberOfQuestions, cancelled, tournamentCreator, tournamentParticipants, tournamentExecution, tournamentTopics, tournamentQuiz);
            tournament = tournamentRepository.save(tournament);
            return new TournamentDto(tournament);
        } catch (Exception e) {
            throw new AnswersException("Error creating tournament: " + e.getMessage());
        }
    }

    public TournamentDto getTournamentById(Integer id) {
        try {
            Tournament tournament = (Tournament) tournamentRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Tournament not found with id: " + id));
            return new TournamentDto(tournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving tournament: " + e.getMessage());
        }
    }

    public List<TournamentDto> getAllTournaments() {
        try {
            return tournamentRepository.findAll().stream()
                .map(entity -> new TournamentDto((Tournament) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all tournaments: " + e.getMessage());
        }
    }

    public TournamentDto updateTournament(Integer id, TournamentDto tournamentDto) {
        try {
            Tournament tournament = (Tournament) tournamentRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Tournament not found with id: " + id));
            
                        if (tournamentDto.getStartTime() != null) {
                tournament.setStartTime(tournamentDto.getStartTime());
            }
            if (tournamentDto.getEndTime() != null) {
                tournament.setEndTime(tournamentDto.getEndTime());
            }
            if (tournamentDto.getNumberOfQuestions() != null) {
                tournament.setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
            }
            tournament.setCancelled(tournamentDto.isCancelled());
            if (tournamentDto.getTournamentCreator() != null) {
                tournament.setTournamentCreator(tournamentDto.getTournamentCreator());
            }
            if (tournamentDto.getTournamentParticipants() != null) {
                tournament.setTournamentParticipants(tournamentDto.getTournamentParticipants());
            }
            if (tournamentDto.getTournamentExecution() != null) {
                tournament.setTournamentExecution(tournamentDto.getTournamentExecution());
            }
            if (tournamentDto.getTournamentTopics() != null) {
                tournament.setTournamentTopics(tournamentDto.getTournamentTopics());
            }
            if (tournamentDto.getTournamentQuiz() != null) {
                tournament.setTournamentQuiz(tournamentDto.getTournamentQuiz());
            }
            
            tournament = tournamentRepository.save(tournament);
            return new TournamentDto(tournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating tournament: " + e.getMessage());
        }
    }

    public void deleteTournament(Integer id) {
        try {
            if (!tournamentRepository.existsById(id)) {
                throw new AnswersException("Tournament not found with id: " + id);
            }
            tournamentRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting tournament: " + e.getMessage());
        }
    }

    // Business Methods
    @Transactional
    public List<TournamentDto> getOpenedTournamentsForExecution(Integer id, Integer executionId, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Tournament not found with id: " + id));
            
            // Business logic for getOpenedTournamentsForExecution
            List<TournamentDto> result = tournament.getOpenedTournamentsForExecution();
            tournamentRepository.save(tournament);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getOpenedTournamentsForExecution: " + e.getMessage());
        }
    }

    @Transactional
    public List<TournamentDto> getClosedTournamentsForExecution(Integer id, Integer executionId, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Tournament not found with id: " + id));
            
            // Business logic for getClosedTournamentsForExecution
            List<TournamentDto> result = tournament.getClosedTournamentsForExecution();
            tournamentRepository.save(tournament);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getClosedTournamentsForExecution: " + e.getMessage());
        }
    }

    @Transactional
    public TournamentParticipant getTournamentParticipant(Integer id, Integer userAggregateId) {
        try {
            Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Tournament not found with id: " + id));
            
            // Business logic for getTournamentParticipant
            TournamentParticipant result = tournament.getTournamentParticipant();
            tournamentRepository.save(tournament);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getTournamentParticipant: " + e.getMessage());
        }
    }

    // Custom Workflow Methods
    @Transactional
    public void addParticipant(Integer userId, Integer tournamentId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for addParticipant
            throw new UnsupportedOperationException("Workflow addParticipant not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow addParticipant: " + e.getMessage());
        }
    }

    @Transactional
    public void leaveTournament(Integer userAggregateId, Integer tournamentId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for leaveTournament
            throw new UnsupportedOperationException("Workflow leaveTournament not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow leaveTournament: " + e.getMessage());
        }
    }

    @Transactional
    public void solveQuiz(Integer userAggregateId, Integer tournamentId, Integer quizAnswerId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for solveQuiz
            throw new UnsupportedOperationException("Workflow solveQuiz not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow solveQuiz: " + e.getMessage());
        }
    }

    @Transactional
    public void cancelTournament(Integer tournamentId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for cancelTournament
            throw new UnsupportedOperationException("Workflow cancelTournament not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow cancelTournament: " + e.getMessage());
        }
    }

    @Transactional
    public void reopenTournament(Integer tournamentId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for reopenTournament
            throw new UnsupportedOperationException("Workflow reopenTournament not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow reopenTournament: " + e.getMessage());
        }
    }

    @Transactional
    public void anonymizeUser(Integer userAggregateId, Integer tournamentId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for anonymizeUser
            throw new UnsupportedOperationException("Workflow anonymizeUser not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow anonymizeUser: " + e.getMessage());
        }
    }

    @Transactional
    public void removeUser(Integer userAggregateId, Integer tournamentId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeUser
            throw new UnsupportedOperationException("Workflow removeUser not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeUser: " + e.getMessage());
        }
    }

    @Transactional
    public void updateTopic(Integer topicAggregateId, Integer tournamentId, String topicName, AggregateState state, Integer version, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for updateTopic
            throw new UnsupportedOperationException("Workflow updateTopic not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow updateTopic: " + e.getMessage());
        }
    }

    @Transactional
    public void removeTopic(Integer topicAggregateId, Integer tournamentId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeTopic
            throw new UnsupportedOperationException("Workflow removeTopic not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeTopic: " + e.getMessage());
        }
    }

    // Query methods not implemented

    // Event Processing Methods
    private void publishTournamentCreatedEvent(Tournament tournament) {
        try {
            // TODO: Implement event publishing for TournamentCreated
            // eventPublisher.publishEvent(new TournamentCreatedEvent(tournament));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish TournamentCreatedEvent", e);
        }
    }

    private void publishTournamentUpdatedEvent(Tournament tournament) {
        try {
            // TODO: Implement event publishing for TournamentUpdated
            // eventPublisher.publishEvent(new TournamentUpdatedEvent(tournament));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish TournamentUpdatedEvent", e);
        }
    }

    private void publishTournamentDeletedEvent(Long tournamentId) {
        try {
            // TODO: Implement event publishing for TournamentDeleted
            // eventPublisher.publishEvent(new TournamentDeletedEvent(tournamentId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish TournamentDeletedEvent", e);
        }
    }
}
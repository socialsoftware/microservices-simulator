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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipantQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTournamentRequestDto;


@Service
@Transactional
public class TournamentService {
    private static final Logger logger = LoggerFactory.getLogger(TournamentService.class);

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentFactory tournamentFactory;

    public TournamentService() {}

    // CRUD Operations
    public TournamentDto createTournament(TournamentCreator creator, TournamentExecution execution, TournamentQuiz quiz, CreateTournamentRequestDto createRequest, Set<TournamentParticipant> participants, Set<TournamentTopic> topics, UnitOfWork unitOfWork) {
        try {
            // Convert CreateRequestDto to regular DTO
            TournamentDto tournamentDto = new TournamentDto();
            tournamentDto.setStartTime(createRequest.getStartTime());
            tournamentDto.setEndTime(createRequest.getEndTime());
            tournamentDto.setNumberOfQuestions(createRequest.getNumberOfQuestions());
            tournamentDto.setCancelled(createRequest.getCancelled());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Tournament tournament = tournamentFactory.createTournament(aggregateId, creator, execution, quiz, tournamentDto, participants, topics);
            unitOfWorkService.registerChanged(tournament, unitOfWork);
            return tournamentFactory.createTournamentDto(tournament);
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

    public TournamentDto updateTournament(TournamentDto tournamentDto) {
        try {
            Integer id = tournamentDto.getAggregateId();
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
            if (tournamentDto.getCreator() != null) {
                tournament.setCreator(tournamentDto.getCreator());
            }
            if (tournamentDto.getParticipants() != null) {
                tournament.setParticipants(tournamentDto.getParticipants());
            }
            if (tournamentDto.getExecution() != null) {
                tournament.setExecution(tournamentDto.getExecution());
            }
            if (tournamentDto.getTopics() != null) {
                tournament.setTopics(tournamentDto.getTopics());
            }
            if (tournamentDto.getQuiz() != null) {
                tournament.setQuiz(tournamentDto.getQuiz());
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

    // No business methods defined

    // No custom workflows defined

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
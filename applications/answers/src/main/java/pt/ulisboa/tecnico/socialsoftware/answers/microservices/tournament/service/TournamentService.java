package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentCreatorDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentQuizDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTournamentRequestDto;


@Service
@Transactional
public class TournamentService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentFactory tournamentFactory;

    public TournamentService() {}

    public TournamentDto createTournament(CreateTournamentRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TournamentDto tournamentDto = new TournamentDto();
            tournamentDto.setStartTime(createRequest.getStartTime());
            tournamentDto.setEndTime(createRequest.getEndTime());
            tournamentDto.setNumberOfQuestions(createRequest.getNumberOfQuestions());
            tournamentDto.setCancelled(createRequest.getCancelled());
            if (createRequest.getCreator() != null) {
                TournamentCreatorDto creatorDto = new TournamentCreatorDto();
                creatorDto.setAggregateId(createRequest.getCreator().getAggregateId());
                creatorDto.setVersion(createRequest.getCreator().getVersion());
                creatorDto.setState(createRequest.getCreator().getState());
                tournamentDto.setCreator(creatorDto);
            }
            if (createRequest.getParticipants() != null) {
                tournamentDto.setParticipants(createRequest.getParticipants().stream().map(srcDto -> {
                    TournamentParticipantDto projDto = new TournamentParticipantDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            if (createRequest.getExecution() != null) {
                TournamentExecutionDto executionDto = new TournamentExecutionDto();
                executionDto.setAggregateId(createRequest.getExecution().getAggregateId());
                executionDto.setVersion(createRequest.getExecution().getVersion());
                executionDto.setState(createRequest.getExecution().getState());
                tournamentDto.setExecution(executionDto);
            }
            if (createRequest.getTopics() != null) {
                tournamentDto.setTopics(createRequest.getTopics().stream().map(srcDto -> {
                    TournamentTopicDto projDto = new TournamentTopicDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            if (createRequest.getQuiz() != null) {
                TournamentQuizDto quizDto = new TournamentQuizDto();
                quizDto.setAggregateId(createRequest.getQuiz().getAggregateId());
                quizDto.setVersion(createRequest.getQuiz().getVersion());
                quizDto.setState(createRequest.getQuiz().getState());
                tournamentDto.setQuiz(quizDto);
            }
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Tournament tournament = tournamentFactory.createTournament(aggregateId, tournamentDto);
            unitOfWorkService.registerChanged(tournament, unitOfWork);
            return tournamentFactory.createTournamentDto(tournament);
        } catch (Exception e) {
            throw new AnswersException("Error creating tournament: " + e.getMessage());
        }
    }

    public TournamentDto getTournamentById(Integer id, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return tournamentFactory.createTournamentDto(tournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving tournament: " + e.getMessage());
        }
    }

    public List<TournamentDto> getAllTournaments(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = tournamentRepository.findAll().stream()
                .map(Tournament::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(tournamentFactory::createTournamentDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all tournaments: " + e.getMessage());
        }
    }

    public TournamentDto updateTournament(TournamentDto tournamentDto, UnitOfWork unitOfWork) {
        try {
            Integer id = tournamentDto.getAggregateId();
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (tournamentDto.getStartTime() != null) {
                tournament.setStartTime(tournamentDto.getStartTime());
            }
            if (tournamentDto.getEndTime() != null) {
                tournament.setEndTime(tournamentDto.getEndTime());
            }
            if (tournamentDto.getNumberOfQuestions() != null) {
                tournament.setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
            }
            tournament.setCancelled(tournamentDto.getCancelled());

            unitOfWorkService.registerChanged(tournament, unitOfWork);
            return tournamentFactory.createTournamentDto(tournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating tournament: " + e.getMessage());
        }
    }

    public void deleteTournament(Integer id, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            tournament.remove();
            unitOfWorkService.registerChanged(tournament, unitOfWork);
            unitOfWorkService.registerEvent(new TournamentDeletedEvent(tournament.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting tournament: " + e.getMessage());
        }
    }




}
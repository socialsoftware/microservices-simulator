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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentCreatorDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentCreatorUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentParticipantDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentParticipantUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentParticipantQuizDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentParticipantQuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentTopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentTopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentQuizDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentQuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentParticipantRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentParticipantUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentTopicRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentTopicUpdatedEvent;
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
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            if (tournamentDto.getStartTime() != null) {
                newTournament.setStartTime(tournamentDto.getStartTime());
            }
            if (tournamentDto.getEndTime() != null) {
                newTournament.setEndTime(tournamentDto.getEndTime());
            }
            if (tournamentDto.getNumberOfQuestions() != null) {
                newTournament.setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
            }
            newTournament.setCancelled(tournamentDto.getCancelled());

            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentUpdatedEvent event = new TournamentUpdatedEvent(newTournament.getAggregateId(), newTournament.getStartTime(), newTournament.getEndTime(), newTournament.getNumberOfQuestions(), newTournament.getCancelled());
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return tournamentFactory.createTournamentDto(newTournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating tournament: " + e.getMessage());
        }
    }

    public void deleteTournament(Integer id, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            newTournament.remove();
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            unitOfWorkService.registerEvent(new TournamentDeletedEvent(newTournament.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting tournament: " + e.getMessage());
        }
    }

    public TournamentParticipantDto addTournamentParticipant(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto TournamentParticipantDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentParticipant element = new TournamentParticipant(TournamentParticipantDto);
            newTournament.getParticipants().add(element);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentParticipantDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentParticipant: " + e.getMessage());
        }
    }

    public List<TournamentParticipantDto> addTournamentParticipants(Integer tournamentId, List<TournamentParticipantDto> TournamentParticipantDtos, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentParticipantDtos.forEach(dto -> {
                TournamentParticipant element = new TournamentParticipant(dto);
                newTournament.getParticipants().add(element);
            });
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentParticipantDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentParticipants: " + e.getMessage());
        }
    }

    public TournamentParticipantDto getTournamentParticipant(Integer tournamentId, Integer participantAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            TournamentParticipant element = tournament.getParticipants().stream()
                .filter(item -> item.getParticipantAggregateId() != null &&
                               item.getParticipantAggregateId().equals(participantAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentParticipant not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving TournamentParticipant: " + e.getMessage());
        }
    }

    public void removeTournamentParticipant(Integer tournamentId, Integer participantAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            newTournament.getParticipants().removeIf(item ->
                item.getParticipantAggregateId() != null &&
                item.getParticipantAggregateId().equals(participantAggregateId)
            );
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentParticipantRemovedEvent event = new TournamentParticipantRemovedEvent(tournamentId, participantAggregateId);
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing TournamentParticipant: " + e.getMessage());
        }
    }

    public TournamentParticipantDto updateTournamentParticipant(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto TournamentParticipantDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentParticipant element = newTournament.getParticipants().stream()
                .filter(item -> item.getParticipantAggregateId() != null &&
                               item.getParticipantAggregateId().equals(participantAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentParticipant not found"));

            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentParticipantUpdatedEvent event = new TournamentParticipantUpdatedEvent(tournamentId, element.getParticipantAggregateId(), element.getParticipantVersion(), element.getParticipantEnrollTime());
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating TournamentParticipant: " + e.getMessage());
        }
    }

    public TournamentTopicDto addTournamentTopic(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto TournamentTopicDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentTopic element = new TournamentTopic(TournamentTopicDto);
            newTournament.getTopics().add(element);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentTopicDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentTopic: " + e.getMessage());
        }
    }

    public List<TournamentTopicDto> addTournamentTopics(Integer tournamentId, List<TournamentTopicDto> TournamentTopicDtos, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentTopicDtos.forEach(dto -> {
                TournamentTopic element = new TournamentTopic(dto);
                newTournament.getTopics().add(element);
            });
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentTopicDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentTopics: " + e.getMessage());
        }
    }

    public TournamentTopicDto getTournamentTopic(Integer tournamentId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            TournamentTopic element = tournament.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null &&
                               item.getTopicAggregateId().equals(topicAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentTopic not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving TournamentTopic: " + e.getMessage());
        }
    }

    public void removeTournamentTopic(Integer tournamentId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            newTournament.getTopics().removeIf(item ->
                item.getTopicAggregateId() != null &&
                item.getTopicAggregateId().equals(topicAggregateId)
            );
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentTopicRemovedEvent event = new TournamentTopicRemovedEvent(tournamentId, topicAggregateId);
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing TournamentTopic: " + e.getMessage());
        }
    }

    public TournamentTopicDto updateTournamentTopic(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto TournamentTopicDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentTopic element = newTournament.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null &&
                               item.getTopicAggregateId().equals(topicAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentTopic not found"));

            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentTopicUpdatedEvent event = new TournamentTopicUpdatedEvent(tournamentId, element.getTopicAggregateId(), element.getTopicVersion());
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating TournamentTopic: " + e.getMessage());
        }
    }


    public Tournament handleExecutionUpdatedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        // Handle execution single reference
        if (newTournament.getExecution() != null && 
            newTournament.getExecution().getExecutionAggregateId() != null &&
            newTournament.getExecution().getExecutionAggregateId().equals(executionAggregateId)) {
            newTournament.getExecution().setExecutionVersion(executionVersion);
        }

            unitOfWorkService.registerChanged(newTournament, unitOfWork);

        unitOfWorkService.registerEvent(
            new TournamentExecutionUpdatedEvent(
                    newTournament.getAggregateId(),
                    executionAggregateId,
                    executionVersion
            ),
            unitOfWork
        );

            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUpdatedEvent: " + e.getMessage());
        }
    }

    public Tournament handleExecutionUserUpdatedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        // Handle creator single reference
        if (newTournament.getCreator() != null && 
            newTournament.getCreator().getCreatorAggregateId() != null &&
            newTournament.getCreator().getCreatorAggregateId().equals(executionuserAggregateId)) {
            newTournament.getCreator().setCreatorVersion(executionuserVersion);
        }

        // Handle participants collection
        if (newTournament.getParticipants() != null) {
            newTournament.getParticipants().stream()
                .filter(item -> item.getParticipantAggregateId() != null && 
                               item.getParticipantAggregateId().equals(executionuserAggregateId))
                .forEach(item -> item.setParticipantVersion(executionuserVersion));
        }

            unitOfWorkService.registerChanged(newTournament, unitOfWork);

        unitOfWorkService.registerEvent(
            new TournamentCreatorUpdatedEvent(
                    newTournament.getAggregateId(),
                    executionuserAggregateId,
                    executionuserVersion
            ),
            unitOfWork
        );

            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUserUpdatedEvent: " + e.getMessage());
        }
    }

    public Tournament handleTopicUpdatedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        // Handle topics collection
        if (newTournament.getTopics() != null) {
            newTournament.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null && 
                               item.getTopicAggregateId().equals(topicAggregateId))
                .forEach(item -> item.setTopicVersion(topicVersion));
        }

            unitOfWorkService.registerChanged(newTournament, unitOfWork);

        unitOfWorkService.registerEvent(
            new TournamentTopicUpdatedEvent(
                    newTournament.getAggregateId(),
                    topicAggregateId,
                    topicVersion
            ),
            unitOfWork
        );

            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling TopicUpdatedEvent: " + e.getMessage());
        }
    }

    public Tournament handleQuizUpdatedEvent(Integer aggregateId, Integer quizAggregateId, Integer quizVersion, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        // Handle quiz single reference
        if (newTournament.getQuiz() != null && 
            newTournament.getQuiz().getQuizAggregateId() != null &&
            newTournament.getQuiz().getQuizAggregateId().equals(quizAggregateId)) {
            newTournament.getQuiz().setQuizVersion(quizVersion);
        }

            unitOfWorkService.registerChanged(newTournament, unitOfWork);

        unitOfWorkService.registerEvent(
            new TournamentQuizUpdatedEvent(
                    newTournament.getAggregateId(),
                    quizAggregateId,
                    quizVersion
            ),
            unitOfWork
        );

            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling QuizUpdatedEvent: " + e.getMessage());
        }
    }




}
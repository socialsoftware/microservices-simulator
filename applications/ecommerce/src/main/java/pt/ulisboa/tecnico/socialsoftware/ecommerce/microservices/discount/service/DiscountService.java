package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.DiscountDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.DiscountUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi.requestDtos.CreateDiscountRequestDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
public class DiscountService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private DiscountFactory discountFactory;

    @Autowired
    private DiscountServiceExtension extension;

    public DiscountService() {}

    public DiscountDto createDiscount(CreateDiscountRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            DiscountDto discountDto = new DiscountDto();
            discountDto.setCode(createRequest.getCode());
            discountDto.setDescription(createRequest.getDescription());
            discountDto.setPercentageOff(createRequest.getPercentageOff());
            discountDto.setActive(createRequest.getActive());
            discountDto.setValidFrom(createRequest.getValidFrom());
            discountDto.setValidUntil(createRequest.getValidUntil());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Discount discount = discountFactory.createDiscount(aggregateId, discountDto);
            unitOfWorkService.registerChanged(discount, unitOfWork);
            return discountFactory.createDiscountDto(discount);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating discount: " + e.getMessage());
        }
    }

    public DiscountDto getDiscountById(Integer id, UnitOfWork unitOfWork) {
        try {
            Discount discount = (Discount) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return discountFactory.createDiscountDto(discount);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving discount: " + e.getMessage());
        }
    }

    public List<DiscountDto> getAllDiscounts(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = discountRepository.findAll().stream()
                .map(Discount::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Discount) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(discountFactory::createDiscountDto)
                .collect(Collectors.toList());
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving discount: " + e.getMessage());
        }
    }

    public DiscountDto updateDiscount(DiscountDto discountDto, UnitOfWork unitOfWork) {
        try {
            Integer id = discountDto.getAggregateId();
            Discount oldDiscount = (Discount) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Discount newDiscount = discountFactory.createDiscountFromExisting(oldDiscount);
            if (discountDto.getCode() != null) {
                newDiscount.setCode(discountDto.getCode());
            }
            if (discountDto.getDescription() != null) {
                newDiscount.setDescription(discountDto.getDescription());
            }
            if (discountDto.getPercentageOff() != null) {
                newDiscount.setPercentageOff(discountDto.getPercentageOff());
            }
            newDiscount.setActive(discountDto.getActive());
            if (discountDto.getValidFrom() != null) {
                newDiscount.setValidFrom(discountDto.getValidFrom());
            }
            if (discountDto.getValidUntil() != null) {
                newDiscount.setValidUntil(discountDto.getValidUntil());
            }

            unitOfWorkService.registerChanged(newDiscount, unitOfWork);            DiscountUpdatedEvent event = new DiscountUpdatedEvent(newDiscount.getAggregateId(), newDiscount.getCode(), newDiscount.getDescription(), newDiscount.getPercentageOff(), newDiscount.getActive(), newDiscount.getValidFrom(), newDiscount.getValidUntil());
            event.setPublisherAggregateVersion(newDiscount.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return discountFactory.createDiscountDto(newDiscount);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating discount: " + e.getMessage());
        }
    }

    public void deleteDiscount(Integer id, UnitOfWork unitOfWork) {
        try {
            Discount oldDiscount = (Discount) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Discount newDiscount = discountFactory.createDiscountFromExisting(oldDiscount);
            newDiscount.remove();
            unitOfWorkService.registerChanged(newDiscount, unitOfWork);            unitOfWorkService.registerEvent(new DiscountDeletedEvent(newDiscount.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting discount: " + e.getMessage());
        }
    }








}
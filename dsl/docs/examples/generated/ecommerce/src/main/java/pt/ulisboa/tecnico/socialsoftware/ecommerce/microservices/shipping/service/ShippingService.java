package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingOrderDto;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.ShippingStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ShippingDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ShippingUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.webapi.requestDtos.CreateShippingRequestDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
public class ShippingService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private ShippingRepository shippingRepository;

    @Autowired
    private ShippingFactory shippingFactory;

    @Autowired
    private ShippingServiceExtension extension;

    public ShippingService() {}

    public ShippingDto createShipping(CreateShippingRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            ShippingDto shippingDto = new ShippingDto();
            shippingDto.setAddress(createRequest.getAddress());
            shippingDto.setCarrier(createRequest.getCarrier());
            shippingDto.setTrackingNumber(createRequest.getTrackingNumber());
            shippingDto.setStatus(createRequest.getStatus() != null ? createRequest.getStatus().name() : null);
            if (createRequest.getOrder() != null) {
                Order refSource = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getOrder().getAggregateId(), unitOfWork);
                OrderDto refSourceDto = new OrderDto(refSource);
                ShippingOrderDto orderDto = new ShippingOrderDto();
                orderDto.setAggregateId(refSourceDto.getAggregateId());
                orderDto.setVersion(refSourceDto.getVersion());
                orderDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                orderDto.setTotalInCents(refSourceDto.getTotalInCents());
                orderDto.setItemCount(refSourceDto.getItemCount());
                shippingDto.setOrder(orderDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Shipping shipping = shippingFactory.createShipping(aggregateId, shippingDto);
            unitOfWorkService.registerChanged(shipping, unitOfWork);
            return shippingFactory.createShippingDto(shipping);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating shipping: " + e.getMessage());
        }
    }

    public ShippingDto getShippingById(Integer id, UnitOfWork unitOfWork) {
        try {
            Shipping shipping = (Shipping) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return shippingFactory.createShippingDto(shipping);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving shipping: " + e.getMessage());
        }
    }

    public List<ShippingDto> getAllShippings(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = shippingRepository.findAll().stream()
                .map(Shipping::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Shipping) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(shippingFactory::createShippingDto)
                .collect(Collectors.toList());
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving shipping: " + e.getMessage());
        }
    }

    public ShippingDto updateShipping(ShippingDto shippingDto, UnitOfWork unitOfWork) {
        try {
            Integer id = shippingDto.getAggregateId();
            Shipping oldShipping = (Shipping) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Shipping newShipping = shippingFactory.createShippingFromExisting(oldShipping);
            if (shippingDto.getAddress() != null) {
                newShipping.setAddress(shippingDto.getAddress());
            }
            if (shippingDto.getCarrier() != null) {
                newShipping.setCarrier(shippingDto.getCarrier());
            }
            if (shippingDto.getTrackingNumber() != null) {
                newShipping.setTrackingNumber(shippingDto.getTrackingNumber());
            }
            if (shippingDto.getStatus() != null) {
                newShipping.setStatus(ShippingStatus.valueOf(shippingDto.getStatus()));
            }

            unitOfWorkService.registerChanged(newShipping, unitOfWork);            ShippingUpdatedEvent event = new ShippingUpdatedEvent(newShipping.getAggregateId(), newShipping.getAddress(), newShipping.getCarrier(), newShipping.getTrackingNumber());
            event.setPublisherAggregateVersion(newShipping.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return shippingFactory.createShippingDto(newShipping);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating shipping: " + e.getMessage());
        }
    }

    public void deleteShipping(Integer id, UnitOfWork unitOfWork) {
        try {
            Shipping oldShipping = (Shipping) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Shipping newShipping = shippingFactory.createShippingFromExisting(oldShipping);
            newShipping.remove();
            unitOfWorkService.registerChanged(newShipping, unitOfWork);            unitOfWorkService.registerEvent(new ShippingDeletedEvent(newShipping.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting shipping: " + e.getMessage());
        }
    }




    public void handlePaymentAuthorizedEvent(Integer aggregateId, UnitOfWork unitOfWork) {
    }

    public void handleOrderCancelledEvent(Integer aggregateId, UnitOfWork unitOfWork) {
    }




}
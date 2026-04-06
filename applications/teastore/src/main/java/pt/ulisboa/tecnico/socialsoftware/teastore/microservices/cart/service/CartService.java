package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CartDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CartUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto;


@Service
@Transactional
public class CartService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartFactory cartFactory;

    public CartService() {}

    public CartDto createCart(CreateCartRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            CartDto cartDto = new CartDto();
            cartDto.setUserId(createRequest.getUserId());
            cartDto.setCheckedOut(createRequest.getCheckedOut());
            cartDto.setTotalPrice(createRequest.getTotalPrice());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Cart cart = cartFactory.createCart(aggregateId, cartDto);
            unitOfWorkService.registerChanged(cart, unitOfWork);
            return cartFactory.createCartDto(cart);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error creating cart: " + e.getMessage());
        }
    }

    public CartDto getCartById(Integer id, UnitOfWork unitOfWork) {
        try {
            Cart cart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return cartFactory.createCartDto(cart);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving cart: " + e.getMessage());
        }
    }

    public List<CartDto> getAllCarts(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = cartRepository.findAll().stream()
                .map(Cart::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(cartFactory::createCartDto)
                .collect(Collectors.toList());
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving cart: " + e.getMessage());
        }
    }

    public CartDto updateCart(CartDto cartDto, UnitOfWork unitOfWork) {
        try {
            Integer id = cartDto.getAggregateId();
            Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Cart newCart = cartFactory.createCartFromExisting(oldCart);
            if (cartDto.getUserId() != null) {
                newCart.setUserId(cartDto.getUserId());
            }
            newCart.setCheckedOut(cartDto.getCheckedOut());
            if (cartDto.getTotalPrice() != null) {
                newCart.setTotalPrice(cartDto.getTotalPrice());
            }

            unitOfWorkService.registerChanged(newCart, unitOfWork);            CartUpdatedEvent event = new CartUpdatedEvent(newCart.getAggregateId(), newCart.getUserId(), newCart.getCheckedOut(), newCart.getTotalPrice());
            event.setPublisherAggregateVersion(newCart.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return cartFactory.createCartDto(newCart);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error updating cart: " + e.getMessage());
        }
    }

    public void deleteCart(Integer id, UnitOfWork unitOfWork) {
        try {
            Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Cart newCart = cartFactory.createCartFromExisting(oldCart);
            newCart.remove();
            unitOfWorkService.registerChanged(newCart, unitOfWork);            unitOfWorkService.registerEvent(new CartDeletedEvent(newCart.getAggregateId()), unitOfWork);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error deleting cart: " + e.getMessage());
        }
    }








}
package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartUserDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.CartDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.CartUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.CartUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
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
            cartDto.setTotalInCents(createRequest.getTotalInCents());
            cartDto.setItemCount(createRequest.getItemCount());
            cartDto.setCheckedOut(createRequest.getCheckedOut());
            if (createRequest.getUser() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getUser().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                CartUserDto userDto = new CartUserDto();
                userDto.setAggregateId(refSourceDto.getAggregateId());
                userDto.setVersion(refSourceDto.getVersion());
                userDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                userDto.setUsername(refSourceDto.getUsername());
                userDto.setEmail(refSourceDto.getEmail());
                cartDto.setUser(userDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Cart cart = cartFactory.createCart(aggregateId, cartDto);
            unitOfWorkService.registerChanged(cart, unitOfWork);
            return cartFactory.createCartDto(cart);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating cart: " + e.getMessage());
        }
    }

    public CartDto getCartById(Integer id, UnitOfWork unitOfWork) {
        try {
            Cart cart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return cartFactory.createCartDto(cart);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving cart: " + e.getMessage());
        }
    }

    public List<CartDto> getAllCarts(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = cartRepository.findAll().stream()
                .map(Cart::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(cartFactory::createCartDto)
                .collect(Collectors.toList());
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving cart: " + e.getMessage());
        }
    }

    public CartDto updateCart(CartDto cartDto, UnitOfWork unitOfWork) {
        try {
            Integer id = cartDto.getAggregateId();
            Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Cart newCart = cartFactory.createCartFromExisting(oldCart);
            if (cartDto.getTotalInCents() != null) {
                newCart.setTotalInCents(cartDto.getTotalInCents());
            }
            if (cartDto.getItemCount() != null) {
                newCart.setItemCount(cartDto.getItemCount());
            }
            newCart.setCheckedOut(cartDto.getCheckedOut());

            unitOfWorkService.registerChanged(newCart, unitOfWork);            CartUpdatedEvent event = new CartUpdatedEvent(newCart.getAggregateId(), newCart.getTotalInCents(), newCart.getItemCount(), newCart.getCheckedOut());
            event.setPublisherAggregateVersion(newCart.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return cartFactory.createCartDto(newCart);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating cart: " + e.getMessage());
        }
    }

    public void deleteCart(Integer id, UnitOfWork unitOfWork) {
        try {
            Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Cart newCart = cartFactory.createCartFromExisting(oldCart);
            newCart.remove();
            unitOfWorkService.registerChanged(newCart, unitOfWork);            unitOfWorkService.registerEvent(new CartDeletedEvent(newCart.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting cart: " + e.getMessage());
        }
    }




    public Cart handleUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userEmail, UnitOfWork unitOfWork) {
        try {
            Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Cart newCart = cartFactory.createCartFromExisting(oldCart);



            unitOfWorkService.registerChanged(newCart, unitOfWork);

        unitOfWorkService.registerEvent(
            new CartUserUpdatedEvent(
                    newCart.getAggregateId(),
                    userAggregateId,
                    userVersion,
                    userName,
                    userEmail
            ),
            unitOfWork
        );

            return newCart;
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error handling UserUpdatedEvent cart: " + e.getMessage());
        }
    }




}
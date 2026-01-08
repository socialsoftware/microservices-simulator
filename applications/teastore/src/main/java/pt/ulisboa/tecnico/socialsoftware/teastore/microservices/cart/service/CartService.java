package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish.CartUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish.CartDeletedEvent;

@Service
public class CartService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final CartRepository cartRepository;

    @Autowired
    private CartFactory cartFactory;

    public CartService(UnitOfWorkService unitOfWorkService, CartRepository cartRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.cartRepository = cartRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CartDto createCart(CartDto cartDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Cart cart = cartFactory.createCart(aggregateId, cartDto);
        unitOfWorkService.registerChanged(cart, unitOfWork);
        return cartFactory.createCartDto(cart);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CartDto getCartById(Integer aggregateId, UnitOfWork unitOfWork) {
        return cartFactory.createCartDto((Cart) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CartDto updateCart(CartDto cartDto, UnitOfWork unitOfWork) {
        Integer aggregateId = cartDto.getAggregateId();
        Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Cart newCart = cartFactory.createCartFromExisting(oldCart);
        newCart.setUserId(cartDto.getUserId());
        newCart.setCheckedOut(cartDto.getCheckedOut());
        newCart.setTotalPrice(cartDto.getTotalPrice());
        unitOfWorkService.registerChanged(newCart, unitOfWork);
        unitOfWorkService.registerEvent(new CartUpdatedEvent(newCart.getAggregateId(), newCart.getUserId(), newCart.getCheckedOut(), newCart.getTotalPrice()), unitOfWork);
        return cartFactory.createCartDto(newCart);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteCart(Integer aggregateId, UnitOfWork unitOfWork) {
        Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Cart newCart = cartFactory.createCartFromExisting(oldCart);
        newCart.remove();
        unitOfWorkService.registerChanged(newCart, unitOfWork);
        unitOfWorkService.registerEvent(new CartDeletedEvent(newCart.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<CartDto> searchCarts(Boolean checkedOut, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = cartRepository.findAll().stream()
                .filter(entity -> {
                    if (checkedOut != null) {
                        if (entity.getCheckedOut() != checkedOut) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(Cart::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(cartFactory::createCartDto)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cart addItem(Long cartAggregateId, Long productId, String productName, Double unitPriceInCents, Integer quantity, UnitOfWork unitOfWork) {
        // TODO: Implement addItem method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cart updateItem(Long cartAggregateId, Long productId, Integer quantity, UnitOfWork unitOfWork) {
        // TODO: Implement updateItem method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cart removeItem(Long cartAggregateId, Long productId, UnitOfWork unitOfWork) {
        // TODO: Implement removeItem method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cart checkoutCart(Long cartAggregateId, UnitOfWork unitOfWork) {
        // TODO: Implement checkoutCart method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cart findCartById(Long cartAggregateId, UnitOfWork unitOfWork) {
        // TODO: Implement findCartById method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cart findByUserId(Long userId, UnitOfWork unitOfWork) {
        // TODO: Implement findByUserId method
        return null;
    }

}

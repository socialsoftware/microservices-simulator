package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
public class UserService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    @Autowired
    private UserServiceExtension extension;

    @Autowired
    private ApplicationContext applicationContext;

    public UserService() {}

    public UserDto createUser(CreateUserRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            UserDto userDto = new UserDto();
            userDto.setUsername(createRequest.getUsername());
            userDto.setEmail(createRequest.getEmail());
            userDto.setPasswordHash(createRequest.getPasswordHash());
            userDto.setShippingAddress(createRequest.getShippingAddress());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            User user = userFactory.createUser(aggregateId, userDto);
            unitOfWorkService.registerChanged(user, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto getUserById(Integer id, UnitOfWork unitOfWork) {
        try {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving user: " + e.getMessage());
        }
    }

    public List<UserDto> getAllUsers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = userRepository.findAll().stream()
                .map(User::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(userFactory::createUserDto)
                .collect(Collectors.toList());
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving user: " + e.getMessage());
        }
    }

    public UserDto updateUser(UserDto userDto, UnitOfWork unitOfWork) {
        try {
            Integer id = userDto.getAggregateId();
            User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            User newUser = userFactory.createUserFromExisting(oldUser);
            if (userDto.getUsername() != null) {
                newUser.setUsername(userDto.getUsername());
            }
            if (userDto.getEmail() != null) {
                newUser.setEmail(userDto.getEmail());
            }
            if (userDto.getPasswordHash() != null) {
                newUser.setPasswordHash(userDto.getPasswordHash());
            }
            if (userDto.getShippingAddress() != null) {
                newUser.setShippingAddress(userDto.getShippingAddress());
            }

            unitOfWorkService.registerChanged(newUser, unitOfWork);            UserUpdatedEvent event = new UserUpdatedEvent(newUser.getAggregateId(), newUser.getUsername(), newUser.getEmail(), newUser.getShippingAddress());
            event.setPublisherAggregateVersion(newUser.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return userFactory.createUserDto(newUser);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(Integer id, UnitOfWork unitOfWork) {
        try {
            CartRepository cartRepositoryRef = applicationContext.getBean(CartRepository.class);
            boolean hasCartReferences = cartRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != User.AggregateState.DELETED)
                .anyMatch(s -> s.getUser() != null && id.equals(s.getUser().getUserAggregateId()));
            if (hasCartReferences) {
                throw new EcommerceException("Cannot delete user that has a cart");
            }
            InvoiceRepository invoiceRepositoryRef = applicationContext.getBean(InvoiceRepository.class);
            boolean hasInvoiceReferences = invoiceRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != User.AggregateState.DELETED)
                .anyMatch(s -> s.getUser() != null && id.equals(s.getUser().getUserAggregateId()));
            if (hasInvoiceReferences) {
                throw new EcommerceException("Cannot delete user that has invoices");
            }
            OrderRepository orderRepositoryRef = applicationContext.getBean(OrderRepository.class);
            boolean hasOrderReferences = orderRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != User.AggregateState.DELETED)
                .anyMatch(s -> s.getUser() != null && id.equals(s.getUser().getUserAggregateId()));
            if (hasOrderReferences) {
                throw new EcommerceException("Cannot delete user that has orders");
            }
            WishlistItemRepository wishlistitemRepositoryRef = applicationContext.getBean(WishlistItemRepository.class);
            boolean hasWishlistItemReferences = wishlistitemRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != User.AggregateState.DELETED)
                .anyMatch(s -> s.getUser() != null && id.equals(s.getUser().getUserAggregateId()));
            if (hasWishlistItemReferences) {
                throw new EcommerceException("Cannot delete user that has wishlist items");
            }
            User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            User newUser = userFactory.createUserFromExisting(oldUser);
            newUser.remove();
            unitOfWorkService.registerChanged(newUser, unitOfWork);            unitOfWorkService.registerEvent(new UserDeletedEvent(newUser.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting user: " + e.getMessage());
        }
    }





    @Transactional
    public UserDto makeUser(String username, String email, String shippingAddress, UnitOfWork unitOfWork) {
        try {
        UserDto dto = new UserDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setPasswordHash("seeded");
        dto.setShippingAddress(shippingAddress);
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, dto);
        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error in makeUser User: " + e.getMessage());
        }
    }


}
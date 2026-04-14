package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemUserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemProductDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.WishlistItemDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.WishlistItemUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.WishlistItemUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.WishlistItemProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi.requestDtos.CreateWishlistItemRequestDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ProductDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
public class WishlistItemService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private WishlistItemRepository wishlistitemRepository;

    @Autowired
    private WishlistItemFactory wishlistitemFactory;

    @Autowired
    private WishlistItemServiceExtension extension;

    public WishlistItemService() {}

    public WishlistItemDto createWishlistItem(CreateWishlistItemRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            WishlistItemDto wishlistitemDto = new WishlistItemDto();
            wishlistitemDto.setAddedAt(createRequest.getAddedAt());
            if (createRequest.getUser() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getUser().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                WishlistItemUserDto userDto = new WishlistItemUserDto();
                userDto.setAggregateId(refSourceDto.getAggregateId());
                userDto.setVersion(refSourceDto.getVersion());
                userDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                userDto.setUsername(refSourceDto.getUsername());
                userDto.setEmail(refSourceDto.getEmail());
                wishlistitemDto.setUser(userDto);
            }
            if (createRequest.getProduct() != null) {
                Product refSource = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getProduct().getAggregateId(), unitOfWork);
                ProductDto refSourceDto = new ProductDto(refSource);
                WishlistItemProductDto productDto = new WishlistItemProductDto();
                productDto.setAggregateId(refSourceDto.getAggregateId());
                productDto.setVersion(refSourceDto.getVersion());
                productDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                productDto.setSku(refSourceDto.getSku());
                productDto.setName(refSourceDto.getName());
                productDto.setPriceInCents(refSourceDto.getPriceInCents());
                wishlistitemDto.setProduct(productDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            WishlistItem wishlistitem = wishlistitemFactory.createWishlistItem(aggregateId, wishlistitemDto);
            unitOfWorkService.registerChanged(wishlistitem, unitOfWork);
            return wishlistitemFactory.createWishlistItemDto(wishlistitem);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating wishlistitem: " + e.getMessage());
        }
    }

    public WishlistItemDto getWishlistItemById(Integer id, UnitOfWork unitOfWork) {
        try {
            WishlistItem wishlistitem = (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return wishlistitemFactory.createWishlistItemDto(wishlistitem);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving wishlistitem: " + e.getMessage());
        }
    }

    public List<WishlistItemDto> getAllWishlistItems(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = wishlistitemRepository.findAll().stream()
                .map(WishlistItem::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(wishlistitemFactory::createWishlistItemDto)
                .collect(Collectors.toList());
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving wishlistitem: " + e.getMessage());
        }
    }

    public WishlistItemDto updateWishlistItem(WishlistItemDto wishlistitemDto, UnitOfWork unitOfWork) {
        try {
            Integer id = wishlistitemDto.getAggregateId();
            WishlistItem oldWishlistItem = (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            WishlistItem newWishlistItem = wishlistitemFactory.createWishlistItemFromExisting(oldWishlistItem);
            if (wishlistitemDto.getAddedAt() != null) {
                newWishlistItem.setAddedAt(wishlistitemDto.getAddedAt());
            }

            unitOfWorkService.registerChanged(newWishlistItem, unitOfWork);            WishlistItemUpdatedEvent event = new WishlistItemUpdatedEvent(newWishlistItem.getAggregateId(), newWishlistItem.getAddedAt());
            event.setPublisherAggregateVersion(newWishlistItem.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return wishlistitemFactory.createWishlistItemDto(newWishlistItem);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating wishlistitem: " + e.getMessage());
        }
    }

    public void deleteWishlistItem(Integer id, UnitOfWork unitOfWork) {
        try {
            WishlistItem oldWishlistItem = (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            WishlistItem newWishlistItem = wishlistitemFactory.createWishlistItemFromExisting(oldWishlistItem);
            newWishlistItem.remove();
            unitOfWorkService.registerChanged(newWishlistItem, unitOfWork);            unitOfWorkService.registerEvent(new WishlistItemDeletedEvent(newWishlistItem.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting wishlistitem: " + e.getMessage());
        }
    }




    public WishlistItem handleUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userEmail, UnitOfWork unitOfWork) {
        try {
            WishlistItem oldWishlistItem = (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            WishlistItem newWishlistItem = wishlistitemFactory.createWishlistItemFromExisting(oldWishlistItem);



            unitOfWorkService.registerChanged(newWishlistItem, unitOfWork);

        unitOfWorkService.registerEvent(
            new WishlistItemUserUpdatedEvent(
                    newWishlistItem.getAggregateId(),
                    userAggregateId,
                    userVersion,
                    userName,
                    userEmail
            ),
            unitOfWork
        );

            return newWishlistItem;
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error handling UserUpdatedEvent wishlistitem: " + e.getMessage());
        }
    }

    public WishlistItem handleProductUpdatedEvent(Integer aggregateId, Integer productAggregateId, Integer productVersion, String productSku, String productName, Double productPriceInCents, UnitOfWork unitOfWork) {
        try {
            WishlistItem oldWishlistItem = (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            WishlistItem newWishlistItem = wishlistitemFactory.createWishlistItemFromExisting(oldWishlistItem);



            unitOfWorkService.registerChanged(newWishlistItem, unitOfWork);

        unitOfWorkService.registerEvent(
            new WishlistItemProductUpdatedEvent(
                    newWishlistItem.getAggregateId(),
                    productAggregateId,
                    productVersion,
                    productSku,
                    productName,
                    productPriceInCents
            ),
            unitOfWork
        );

            return newWishlistItem;
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error handling ProductUpdatedEvent wishlistitem: " + e.getMessage());
        }
    }




}
package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;

public interface DiscountFactory {
    Discount createDiscount(Integer aggregateId, DiscountDto discountDto);
    Discount createDiscountFromExisting(Discount existingDiscount);
    DiscountDto createDiscountDto(Discount discount);
}

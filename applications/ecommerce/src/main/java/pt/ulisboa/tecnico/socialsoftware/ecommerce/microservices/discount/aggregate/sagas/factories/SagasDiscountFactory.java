package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.Discount;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.DiscountFactory;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.SagaDiscount;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.dtos.SagaDiscountDto;

@Service
@Profile("sagas")
public class SagasDiscountFactory implements DiscountFactory {
    @Override
    public Discount createDiscount(Integer aggregateId, DiscountDto discountDto) {
        return new SagaDiscount(aggregateId, discountDto);
    }

    @Override
    public Discount createDiscountFromExisting(Discount existingDiscount) {
        return new SagaDiscount((SagaDiscount) existingDiscount);
    }

    @Override
    public DiscountDto createDiscountDto(Discount discount) {
        return new SagaDiscountDto(discount);
    }
}
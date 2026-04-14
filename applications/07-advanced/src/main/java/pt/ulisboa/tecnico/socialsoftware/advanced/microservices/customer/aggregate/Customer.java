package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Customer extends Aggregate {
    private String name;
    private String email;
    private Boolean active;

    public Customer() {

    }

    public Customer(Integer aggregateId, CustomerDto customerDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(customerDto.getName());
        setEmail(customerDto.getEmail());
        setActive(customerDto.getActive());
    }


    public Customer(Customer other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());
        setActive(other.getActive());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return this.name != null && this.name.length() > 0;
    }

    private boolean invariantRule1() {
        return this.email != null && this.email.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Customer name cannot be blank");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Customer email cannot be blank");
        }
    }

    public CustomerDto buildDto() {
        CustomerDto dto = new CustomerDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setEmail(getEmail());
        dto.setActive(getActive());
        return dto;
    }
}
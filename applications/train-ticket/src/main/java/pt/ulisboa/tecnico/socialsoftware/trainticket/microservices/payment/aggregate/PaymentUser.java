package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentUserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;

@Entity
public class PaymentUser {
    @Id
    @GeneratedValue
    private Long id;
    private Integer userAggregateId;
    private Long userVersion;
    private AggregateState userState;
    @OneToOne
    private Payment payment;

    public PaymentUser() {

    }

    public PaymentUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
    }

    public PaymentUser(PaymentUserDto paymentUserDto) {
        setUserAggregateId(paymentUserDto.getAggregateId());
        setUserVersion(paymentUserDto.getVersion());
        setUserState(paymentUserDto.getState() != null ? AggregateState.valueOf(paymentUserDto.getState()) : null);
    }

    public PaymentUser(PaymentUser other) {
        setUserAggregateId(other.getUserAggregateId());
        setUserVersion(other.getUserVersion());
        setUserState(other.getUserState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
    }

    public AggregateState getUserState() {
        return userState;
    }

    public void setUserState(AggregateState userState) {
        this.userState = userState;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }




    public PaymentUserDto buildDto() {
        PaymentUserDto dto = new PaymentUserDto();
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}
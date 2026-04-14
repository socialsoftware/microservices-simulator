package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceUserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;

@Entity
public class InvoiceUser {
    @Id
    @GeneratedValue
    private Long id;
    private String userName;
    private String userEmail;
    private Integer userAggregateId;
    private Integer userVersion;
    private AggregateState userState;
    @OneToOne
    private Invoice invoice;

    public InvoiceUser() {

    }

    public InvoiceUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
    }

    public InvoiceUser(InvoiceUserDto invoiceUserDto) {
        setUserName(invoiceUserDto.getUsername());
        setUserEmail(invoiceUserDto.getEmail());
        setUserAggregateId(invoiceUserDto.getAggregateId());
        setUserVersion(invoiceUserDto.getVersion());
        setUserState(invoiceUserDto.getState() != null ? AggregateState.valueOf(invoiceUserDto.getState()) : null);
    }

    public InvoiceUser(InvoiceUser other) {
        setUserName(other.getUserName());
        setUserEmail(other.getUserEmail());
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Integer userVersion) {
        this.userVersion = userVersion;
    }

    public AggregateState getUserState() {
        return userState;
    }

    public void setUserState(AggregateState userState) {
        this.userState = userState;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }




    public InvoiceUserDto buildDto() {
        InvoiceUserDto dto = new InvoiceUserDto();
        dto.setUsername(getUserName());
        dto.setEmail(getUserEmail());
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}
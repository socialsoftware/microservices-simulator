package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.Gender;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class User extends Aggregate {
    private String userName;
    private String password;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;
    private String documentNum;
    private String email;
    private Double balance;

    public User() {

    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setUserName(userDto.getUserName());
        setPassword(userDto.getPassword());
        setGender(Gender.valueOf(userDto.getGender()));
        setDocumentType(DocumentType.valueOf(userDto.getDocumentType()));
        setDocumentNum(userDto.getDocumentNum());
        setEmail(userDto.getEmail());
        setBalance(userDto.getBalance());
    }


    public User(User other) {
        super(other);
        setUserName(other.getUserName());
        setPassword(other.getPassword());
        setGender(other.getGender());
        setDocumentType(other.getDocumentType());
        setDocumentNum(other.getDocumentNum());
        setEmail(other.getEmail());
        setBalance(other.getBalance());
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNum() {
        return documentNum;
    }

    public void setDocumentNum(String documentNum) {
        this.documentNum = documentNum;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantUserNameNotBlank() {
        return this.userName != null && this.userName != null && this.userName.length() > 0;
    }

    private boolean invariantPasswordNotBlank() {
        return this.password != null && this.password != null && this.password.length() > 0;
    }

    private boolean invariantDocumentTypeSet() {
        return this.documentType != null;
    }

    private boolean invariantBalanceNotNegative() {
        return balance >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantUserNameNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "User name cannot be blank");
        }
        if (!invariantPasswordNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Password cannot be blank");
        }
        if (!invariantDocumentTypeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "User must have a document type");
        }
        if (!invariantBalanceNotNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "User balance cannot be negative");
        }
    }

    public UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUserName(getUserName());
        dto.setPassword(getPassword());
        dto.setGender(getGender() != null ? getGender().name() : null);
        dto.setDocumentType(getDocumentType() != null ? getDocumentType().name() : null);
        dto.setDocumentNum(getDocumentNum());
        dto.setEmail(getEmail());
        dto.setBalance(getBalance());
        return dto;
    }
}
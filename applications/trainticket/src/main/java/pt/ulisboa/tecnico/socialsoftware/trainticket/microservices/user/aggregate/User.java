package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.USER_DOCUMENT_NUMBER_PRESENT;

@Entity
@Table(name = "users")
public abstract class User extends Aggregate {
    // USER_NAME_FINAL: enforced by the compiler, so there is no setter and no runtime check.
    @Column(name = "user_name")
    private final String userName;
    private String password;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;
    @Column(name = "document_number")
    private String documentNumber;
    private String email;

    public User() {
        this.userName = null;
    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        this.userName = userDto.getUserName();
        setPassword(userDto.getPassword());
        setGender(userDto.getGender());
        setDocumentType(userDto.getDocumentType());
        setDocumentNumber(userDto.getDocumentNumber());
        setEmail(userDto.getEmail());
        setAggregateType(getClass().getSimpleName());
    }

    public User(User other) {
        super(other);
        this.userName = other.getUserName();
        setPassword(other.getPassword());
        setGender(other.getGender());
        setDocumentType(other.getDocumentType());
        setDocumentNumber(other.getDocumentNumber());
        setEmail(other.getEmail());
    }

    @Override
    public void verifyInvariants() {
        if (this.documentType != DocumentType.NONE
                && (this.documentNumber == null || this.documentNumber.isBlank())) {
            throw new TrainticketException(USER_DOCUMENT_NUMBER_PRESENT);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Gender getGender() {
        return this.gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public DocumentType getDocumentType() {
        return this.documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return this.documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

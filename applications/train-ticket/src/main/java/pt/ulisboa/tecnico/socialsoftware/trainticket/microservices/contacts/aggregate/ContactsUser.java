package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsUserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;

@Entity
public class ContactsUser {
    @Id
    @GeneratedValue
    private Long id;
    private Integer userAggregateId;
    private Long userVersion;
    private AggregateState userState;
    @OneToOne
    private Contacts contacts;

    public ContactsUser() {

    }

    public ContactsUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
    }

    public ContactsUser(ContactsUserDto contactsUserDto) {
        setUserAggregateId(contactsUserDto.getAggregateId());
        setUserVersion(contactsUserDto.getVersion());
        setUserState(contactsUserDto.getState() != null ? AggregateState.valueOf(contactsUserDto.getState()) : null);
    }

    public ContactsUser(ContactsUser other) {
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

    public Contacts getContacts() {
        return contacts;
    }

    public void setContacts(Contacts contacts) {
        this.contacts = contacts;
    }




    public ContactsUserDto buildDto() {
        ContactsUserDto dto = new ContactsUserDto();
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}
package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateUserRequestDto {
    @NotNull
    private String userName;
    @NotNull
    private String password;
    @NotNull
    private String realName;
    @NotNull
    private String email;

    public CreateUserRequestDto() {}

    public CreateUserRequestDto(String userName, String password, String realName, String email) {
        this.userName = userName;
        this.password = password;
        this.realName = realName;
        this.email = email;
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
    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

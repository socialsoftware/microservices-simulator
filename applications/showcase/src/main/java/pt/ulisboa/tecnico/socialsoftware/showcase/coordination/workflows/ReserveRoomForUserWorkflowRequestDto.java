package pt.ulisboa.tecnico.socialsoftware.showcase.coordination.workflows;

public class ReserveRoomForUserWorkflowRequestDto {
    private String username;
    private String email;
    private Integer roomId;
    private String checkIn;
    private String checkOut;
    private Integer nights;
    private Double price;

    public ReserveRoomForUserWorkflowRequestDto() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}

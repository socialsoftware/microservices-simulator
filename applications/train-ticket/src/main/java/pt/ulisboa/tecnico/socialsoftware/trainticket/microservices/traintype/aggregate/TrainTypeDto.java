package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

public class TrainTypeDto {
    private Integer aggregateId;
    private Long version;
    private String name;
    private Integer economyClassSeats;
    private Integer firstClassSeats;
    private Integer averageSpeed;

    public TrainTypeDto() {
    }

    public TrainTypeDto(Integer aggregateId, Long version, String name, Integer economyClassSeats,
                        Integer firstClassSeats, Integer averageSpeed) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.name = name;
        this.economyClassSeats = economyClassSeats;
        this.firstClassSeats = firstClassSeats;
        this.averageSpeed = averageSpeed;
    }

    public TrainTypeDto(TrainType trainType) {
        this.aggregateId = trainType.getAggregateId();
        this.version = trainType.getVersion();
        this.name = trainType.getName();
        this.economyClassSeats = trainType.getEconomyClassSeats();
        this.firstClassSeats = trainType.getFirstClassSeats();
        this.averageSpeed = trainType.getAverageSpeed();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getEconomyClassSeats() {
        return economyClassSeats;
    }

    public void setEconomyClassSeats(Integer economyClassSeats) {
        this.economyClassSeats = economyClassSeats;
    }

    public Integer getFirstClassSeats() {
        return firstClassSeats;
    }

    public void setFirstClassSeats(Integer firstClassSeats) {
        this.firstClassSeats = firstClassSeats;
    }

    public Integer getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Integer averageSpeed) {
        this.averageSpeed = averageSpeed;
    }
}

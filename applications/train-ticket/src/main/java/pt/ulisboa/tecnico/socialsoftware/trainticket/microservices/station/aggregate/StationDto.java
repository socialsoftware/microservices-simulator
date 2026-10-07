package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

public class StationDto {
    private Integer aggregateId;
    private Long version;
    private String name;
    private Integer stayTime;

    public StationDto() {
    }

    public StationDto(Integer aggregateId, Long version, String name, Integer stayTime) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.name = name;
        this.stayTime = stayTime;
    }

    public StationDto(Station station) {
        this.aggregateId = station.getAggregateId();
        this.version = station.getVersion();
        this.name = station.getName();
        this.stayTime = station.getStayTime();
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

    public Integer getStayTime() {
        return stayTime;
    }

    public void setStayTime(Integer stayTime) {
        this.stayTime = stayTime;
    }
}

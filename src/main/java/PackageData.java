import java.time.Instant;

public class PackageData {

    private Long number = null;
    private Instant timeSec = null;
    private Double serviceData = null;
    private Integer intCRC = null;
    private String check = null;


    public PackageData(Long number, Instant timeSec, Double serviceData, Integer intCRC, String check) {
        this.number = number;
        this.timeSec = timeSec;
        this.serviceData = serviceData;
        this.intCRC = intCRC;
        this.check = check;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public Instant getTimeSec() {
        return timeSec;
    }

    public void setTimeSec(Instant timeSec) {
        this.timeSec = timeSec;
    }

    public Double getServiceData() {
        return serviceData;
    }

    public void setServiceData(Double serviceData) {
        this.serviceData = serviceData;
    }

    public Integer getIntCRC() {
        return intCRC;
    }

    public void setIntCRC(Integer intCRC) {
        this.intCRC = intCRC;
    }

    public String getCheck() {
        return check;
    }

    public void setCheck(String check) {
        this.check = check;
    }
}
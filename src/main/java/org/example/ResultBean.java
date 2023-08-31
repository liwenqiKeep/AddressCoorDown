package org.example;

import java.util.List;

/**
 * @author Liwq
 */
public class ResultBean {

    private String count;

    private List<Geocode> geocodes;

    private String info;

    private String infocode;

    private String status;

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public List<Geocode> getGeocodes() {
        return geocodes;
    }

    public void setGeocodes(List<Geocode> geocodes) {
        this.geocodes = geocodes;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfocode() {
        return infocode;
    }

    public void setInfocode(String infocode) {
        this.infocode = infocode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return "OK".equalsIgnoreCase(info) && Long.parseLong(count) > 0L;
    }
}

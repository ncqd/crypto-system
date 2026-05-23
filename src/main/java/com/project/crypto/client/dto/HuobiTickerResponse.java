package com.project.crypto.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HuobiTickerResponse {

    private String status;
    private List<HuobiTicker> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<HuobiTicker> getData() {
        return data;
    }

    public void setData(List<HuobiTicker> data) {
        this.data = data;
    }
}

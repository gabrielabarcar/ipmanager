package com.example.ipmanager.dto;

public class ValidateNetworkRequest {

    private String ip;
    private Integer cidr;
    private Long id;

    public String getIp() {
        return ip;
    }

    public Integer getCidr() {
        return cidr;
    }

    public Long getId() {
        return id;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setCidr(Integer cidr) {
        this.cidr = cidr;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
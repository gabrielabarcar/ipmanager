package com.example.ipmanager.dto;

public class NetworkForm {

    private Long id;
    private String networkIp;
    private Integer cidr;
    private String enlace;
    private String nombreLugar;
    private String hubSpoke;
    private String wanIp;
    private String wanIp1;
    private String wanIp2;
    private Integer wanCidr;
    private String wanBroadcast;

    public NetworkForm() {
    }

    public Long getId() {
        return id;
    }

    public String getNetworkIp() {
        return networkIp;
    }

    public void setNetworkIp(String networkIp) {
        this.networkIp = networkIp;
    }

    public Integer getCidr() {
        return cidr;
    }

    public void setCidr(Integer cidr) {
        this.cidr = cidr;
    }

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public String getNombreLugar() {
        return nombreLugar;
    }

    public void setNombreLugar(String nombreLugar) {
        this.nombreLugar = nombreLugar;
    }

    public String getHubSpoke() {
        return hubSpoke;
    }

    public void setHubSpoke(String hubSpoke) {
        this.hubSpoke = hubSpoke;
    }

    public String getWanIp() {
        return wanIp;
    }

    public void setWanIp(String wanIp) {
        this.wanIp = wanIp;
    }

    public String getWanIp1() {
        return wanIp1;
    }

    public void setWanIp1(String wanIp1) {
        this.wanIp1 = wanIp1;
    }

    public String getWanIp2() {
        return wanIp2;
    }

    public void setWanIp2(String wanIp2) {
        this.wanIp2 = wanIp2;
    }

    public Integer getWanCidr() {
        return wanCidr;
    }

    public void setWanCidr(Integer wanCidr) {
        this.wanCidr = wanCidr;
    }

    public String getWanBroadcast() {
        return wanBroadcast;
    }

    public void setWanBroadcast(String wanBroadcast) {
        this.wanBroadcast = wanBroadcast;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
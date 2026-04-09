package com.example.ipmanager.dto;

public class ValidateEnlaceRequest {

    private String enlace;
    private Long id;

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
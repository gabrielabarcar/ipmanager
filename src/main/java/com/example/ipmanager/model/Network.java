package com.example.ipmanager.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "networks")
@EntityListeners(AuditingEntityListener.class)
public class Network {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreLugar;
    private String enlace;

    @Column(name = "network_ip")
    private String networkIp;

    private Integer cidr;

    private String wanIp;
    private Integer wanCidr;
    private String wanIp1;
    private String wanIp2;
    private String wanBroadcast;

    private Boolean esHistorico = false;

    // === NUEVOS CAMPOS PARA ACEPTAR TRASLAPE ===
    @Column(name = "acepta_traslape")
    private Boolean aceptaTraslape = false;

    @Column(name = "motivo_justificacion", length = 2000)
    private String motivoJustificacion;

    @Column(name = "traslape_aceptado_por", length = 100)
    private String traslapeAceptadoPor;

    @Column(name = "traslape_aceptado_at")
    private LocalDateTime traslapeAceptadoAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private String updatedBy;

    @Transient
    private Boolean tieneConflicto = false;

    @Transient
    private String detalleConflicto;

    @Transient
    private Integer displayIndex;

    @Transient
    private Boolean tieneLanConflicto = false;

    @Transient
    private Boolean tieneWanConflicto = false;

    @Transient
    private String lanConflictExplanation;

    @Transient
    private String wanConflictExplanation;

    public Network() {
    }

    @PrePersist
    public void prePersist() {
        if (esHistorico == null) {
            esHistorico = false;
        }
        if (aceptaTraslape == null) {
            aceptaTraslape = false;
        }
    }

    public Long getId() {
        return id;
    }

    public String getNombreLugar() {
        return nombreLugar;
    }

    public String getEnlace() {
        return enlace;
    }

    public String getNetworkIp() {
        return networkIp;
    }

    public Integer getCidr() {
        return cidr;
    }

    public String getWanIp() {
        return wanIp;
    }

    public Integer getWanCidr() {
        return wanCidr;
    }

    public String getWanIp1() {
        return wanIp1;
    }

    public String getWanIp2() {
        return wanIp2;
    }

    public String getWanBroadcast() {
        return wanBroadcast;
    }

    public Boolean getEsHistorico() {
        return esHistorico;
    }

    public Boolean getAceptaTraslape() {
        return aceptaTraslape;
    }

    public String getMotivoJustificacion() {
        return motivoJustificacion;
    }

    public String getTraslapeAceptadoPor() {
        return traslapeAceptadoPor;
    }

    public LocalDateTime getTraslapeAceptadoAt() {
        return traslapeAceptadoAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Boolean getTieneConflicto() {
        return tieneConflicto;
    }

    public String getDetalleConflicto() {
        return detalleConflicto;
    }

    public Integer getDisplayIndex() {
        return displayIndex;
    }

    public Boolean getTieneLanConflicto() {
        return tieneLanConflicto;
    }

    public Boolean getTieneWanConflicto() {
        return tieneWanConflicto;
    }

    public String getLanConflictExplanation() {
        return lanConflictExplanation;
    }

    public String getWanConflictExplanation() {
        return wanConflictExplanation;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombreLugar(String nombreLugar) {
        this.nombreLugar = nombreLugar;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public void setNetworkIp(String networkIp) {
        this.networkIp = networkIp;
    }

    public void setCidr(Integer cidr) {
        this.cidr = cidr;
    }

    public void setWanIp(String wanIp) {
        this.wanIp = wanIp;
    }

    public void setWanCidr(Integer wanCidr) {
        this.wanCidr = wanCidr;
    }

    public void setWanIp1(String wanIp1) {
        this.wanIp1 = wanIp1;
    }

    public void setWanIp2(String wanIp2) {
        this.wanIp2 = wanIp2;
    }

    public void setWanBroadcast(String wanBroadcast) {
        this.wanBroadcast = wanBroadcast;
    }

    public void setEsHistorico(Boolean esHistorico) {
        this.esHistorico = esHistorico;
    }

    public void setAceptaTraslape(Boolean aceptaTraslape) {
        this.aceptaTraslape = aceptaTraslape;
    }

    public void setMotivoJustificacion(String motivoJustificacion) {
        this.motivoJustificacion = motivoJustificacion;
    }

    public void setTraslapeAceptadoPor(String traslapeAceptadoPor) {
        this.traslapeAceptadoPor = traslapeAceptadoPor;
    }

    public void setTraslapeAceptadoAt(LocalDateTime traslapeAceptadoAt) {
        this.traslapeAceptadoAt = traslapeAceptadoAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setTieneConflicto(Boolean tieneConflicto) {
        this.tieneConflicto = tieneConflicto;
    }

    public void setDetalleConflicto(String detalleConflicto) {
        this.detalleConflicto = detalleConflicto;
    }

    public void setDisplayIndex(Integer displayIndex) {
        this.displayIndex = displayIndex;
    }

    public void setTieneLanConflicto(Boolean tieneLanConflicto) {
        this.tieneLanConflicto = tieneLanConflicto;
    }

    public void setTieneWanConflicto(Boolean tieneWanConflicto) {
        this.tieneWanConflicto = tieneWanConflicto;
    }

    public void setLanConflictExplanation(String lanConflictExplanation) {
        this.lanConflictExplanation = lanConflictExplanation;
    }

    public void setWanConflictExplanation(String wanConflictExplanation) {
        this.wanConflictExplanation = wanConflictExplanation;
    }
}
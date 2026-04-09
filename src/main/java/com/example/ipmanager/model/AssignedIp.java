package com.example.ipmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assigned_ips",
       uniqueConstraints = @UniqueConstraint(columnNames = {"network_id", "ip_address"}))
public class AssignedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "network_id")
    private Network network;

    @Column(name = "ip_address", nullable = false, length = 15)
    private String ipAddress;

    @Column(name = "ip_as_bigint", nullable = false)
    private Long ipAsBigint;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    public AssignedIp() {}

    public Long getId() { return id; }
    public Network getNetwork() { return network; }
    public void setNetwork(Network network) { this.network = network; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Long getIpAsBigint() { return ipAsBigint; }
    public void setIpAsBigint(Long ipAsBigint) { this.ipAsBigint = ipAsBigint; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
package com.example.ipmanager.repository;

import com.example.ipmanager.model.AssignedIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignedIpRepository extends JpaRepository<AssignedIp, Long> {
    boolean existsByNetworkIdAndIpAddress(Long networkId, String ipAddress);
    List<AssignedIp> findByNetworkIdOrderByAssignedAtDesc(Long networkId);
    Optional<AssignedIp> findTopByNetworkIdOrderByAssignedAtDesc(Long networkId);
}
package com.example.ipmanager.repository;

import com.example.ipmanager.model.Network;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NetworkRepository extends JpaRepository<Network, Long> {

    List<Network> findAllByOrderByIdDesc();

    boolean existsByEnlace(String enlace);

    boolean existsByEnlaceAndIdNot(String enlace, Long id);

    Optional<Network> findById(Long id);
}
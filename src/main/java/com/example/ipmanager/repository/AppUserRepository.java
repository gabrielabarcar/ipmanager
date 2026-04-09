package com.example.ipmanager.repository;

import com.example.ipmanager.model.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    List<AppUser> findAllByOrderByUsernameAsc();

    @EntityGraph(attributePaths = {"moduleAccesses", "moduleAccesses.module"})
    Optional<AppUser> findWithModuleAccessesByUsername(String username);
}
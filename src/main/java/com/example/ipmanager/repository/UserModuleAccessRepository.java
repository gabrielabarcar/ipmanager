package com.example.ipmanager.repository;

import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.model.UserModuleAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserModuleAccessRepository extends JpaRepository<UserModuleAccess, Long> {
    List<UserModuleAccess> findByUser(AppUser user);
    void deleteByUser(AppUser user);
}
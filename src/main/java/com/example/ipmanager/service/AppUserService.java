package com.example.ipmanager.service;

import com.example.ipmanager.model.AppUser;

import java.util.List;

public interface AppUserService {

    List<AppUser> findAll();

    AppUser save(AppUser user, boolean encodePassword);

    AppUser findById(Long id);

    void deleteById(Long id);

    boolean usernameExists(String username, Long currentId);

    boolean correoExists(String correo, Long currentId);
}
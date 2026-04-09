package com.example.ipmanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/debug-text")
    public String debugText() {
        return "DEBUG OK";
    }
}
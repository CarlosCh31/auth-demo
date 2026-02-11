package com.example.auth_demo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResourcesController {

    @GetMapping("/api/r1")
    public Map<String, Object> r1() {
        return Map.of(
            "resource", "Resource 1",
            "message", "Access granted to Resource 1",
            "required_permission", "read:r1"
        );
    }

    @GetMapping("/api/r2")
    public Map<String, Object> r2() {
        return Map.of(
            "resource", "Resource 2",
            "message", "Access granted to Resource 2",
            "required_permission", "read:r2"
        );
    }
}

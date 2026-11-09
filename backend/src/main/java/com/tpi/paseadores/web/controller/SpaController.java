package com.tpi.paseadores.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/login", "/register", "/profile", "/walks", "/walks/{id}", "/walkers/{id}",
            "/pets", "/pets/new", "/pets/{id}/edit", "/bookings", "/my-walks", "/my-walks/new"})
    String frontend() {
        return "forward:/index.html";
    }
}

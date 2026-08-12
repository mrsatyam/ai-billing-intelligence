package com.capstone.billing.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PresentationController {

    @GetMapping("/presentation")
    public String presentation() {
        return "presentation";
    }

    @GetMapping("/capstone")
    public String capstone() {
        return "capstone";
    }
}

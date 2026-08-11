package com.capstone.billing.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "AI Billing Intelligence");
        model.addAttribute("tagline", "Self-learning billing decision engine for P&C insurers");
        return "index";
    }
}

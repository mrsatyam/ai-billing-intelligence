package com.capstone.billing.web;

import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.service.DashboardService;
import com.capstone.billing.service.dto.DashboardMetrics;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        DashboardMetrics metrics = dashboardService.metrics();
        List<AiDecision> recent = dashboardService.recentDecisions(8);
        model.addAttribute("appName", "AI Billing Intelligence");
        model.addAttribute("metrics", metrics);
        model.addAttribute("recentDecisions", recent);
        return "dashboard";
    }
}

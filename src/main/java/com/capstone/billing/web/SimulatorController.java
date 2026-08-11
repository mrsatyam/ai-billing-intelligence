package com.capstone.billing.web;

import com.capstone.billing.service.SimulatorService;
import com.capstone.billing.service.dto.SimulatorResult;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @GetMapping("/simulator")
    public String page(Model model) {
        model.addAttribute("appName", "AI Billing Intelligence");
        return "simulator";
    }

    @PostMapping("/api/simulator/run")
    @ResponseBody
    public ResponseEntity<SimulatorResult> run() {
        return ResponseEntity.ok(simulatorService.run());
    }
}

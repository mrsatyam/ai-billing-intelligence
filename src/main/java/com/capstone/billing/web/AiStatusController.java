package com.capstone.billing.web;

import com.capstone.billing.ai.AiFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiStatusController {

    private final AiFacade aiFacade;

    public AiStatusController(AiFacade aiFacade) {
        this.aiFacade = aiFacade;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean live = aiFacade.isLiveGemini();
        return ResponseEntity.ok(Map.of(
                "provider", live ? "gemini" : "rules",
                "liveGemini", live,
                "mode", live ? "Gemini + rule anchors" : "Rule-based only (set GEMINI_API_KEY or billing.ai.gemini.api-key)"
        ));
    }
}

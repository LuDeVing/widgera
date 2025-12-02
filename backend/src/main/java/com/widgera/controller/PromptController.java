package com.widgera.controller;

import com.widgera.dto.HistoryResponse;
import com.widgera.dto.PromptRequest;
import com.widgera.dto.PromptResponse;
import com.widgera.entity.User;
import com.widgera.service.PromptService;
import com.widgera.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompt")
@RequiredArgsConstructor
@Slf4j
public class PromptController {

    private final PromptService promptService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<PromptResponse> submitPrompt(
            @Valid @RequestBody PromptRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Prompt submission request from user: {}", userDetails.getUsername());
        User user = userService.getUserByUsername(userDetails.getUsername());
        PromptResponse response = promptService.processPrompt(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoryResponse>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("History request from user: {}", userDetails.getUsername());
        User user = userService.getUserByUsername(userDetails.getUsername());
        List<HistoryResponse> history = promptService.getHistory(user, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/all")
    public ResponseEntity<List<HistoryResponse>> getAllHistory(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Full history request from user: {}", userDetails.getUsername());
        User user = userService.getUserByUsername(userDetails.getUsername());
        List<HistoryResponse> history = promptService.getAllHistory(user);
        return ResponseEntity.ok(history);
    }
}

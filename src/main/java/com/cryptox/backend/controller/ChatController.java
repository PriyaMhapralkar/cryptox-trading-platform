package com.cryptox.backend.controller;

import com.cryptox.backend.dto.ChatRequest;
import com.cryptox.backend.dto.ChatResponse;
import com.cryptox.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String reply = chatService.getChatResponse(request.getMessage());
        return new ChatResponse(reply);
    }
}
package com.trabean.notification.api.controller;


import com.trabean.notification.api.dto.request.NotificationCreateReq;
import com.trabean.notification.api.dto.response.NotificationReadRes;
import com.trabean.notification.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")

public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<?> saveNotification(@RequestBody NotificationCreateReq notificationCreateReq) {
        notificationService.saveNotification(notificationCreateReq);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{receiverId}")
    public ResponseEntity<?> getNotification(@PathVariable Long receiverId) {
        List<NotificationReadRes> notificationReadResList = notificationService.findByUserId(receiverId);
        return ResponseEntity.ok().body(notificationReadResList);
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<?> updateIsRead(@PathVariable Long notificationId) {
        notificationService.updateIsReadById(notificationId);
        return ResponseEntity.ok().build();
    }
}

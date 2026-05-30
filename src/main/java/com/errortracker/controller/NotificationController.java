package com.errortracker.controller;

import com.errortracker.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<?> get(@RequestParam(required = false) Boolean bookmarked) {

        return ResponseEntity.ok(service.getNotifications(bookmarked));

    }

    @GetMapping("/count")
    public ResponseEntity<?> count() {

        return ResponseEntity.ok(service.count());

    }

    @PatchMapping("/{id}/bookmark")
    public ResponseEntity<?> bookmark(
            @PathVariable String id
    ) {

        service.toggleBookmark(id);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> read(
            @PathVariable String id
    ) {

        service.markRead(id);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable String id
    ) {

        service.delete(id);

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAll() {

        service.deleteAll();

        return ResponseEntity.noContent()
                .build();
    }

}
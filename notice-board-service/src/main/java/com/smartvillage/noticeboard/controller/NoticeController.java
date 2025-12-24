package com.smartvillage.noticeboard.controller;

import com.smartvillage.noticeboard.entity.Notice;
import com.smartvillage.noticeboard.repository.NoticeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeRepository repo;

    public NoticeController(NoticeRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Notice> list() { return repo.findByIsActiveTrueOrderByNoticeDateDesc(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        Optional<Notice> n = repo.findById(id);
        return n.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Notice create(@RequestBody Notice notice) { return repo.save(notice); }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Notice notice) {
        return repo.findById(id).map(existing -> {
            existing.setTitle(notice.getTitle());
            existing.setDescription(notice.getDescription());
            existing.setCategory(notice.getCategory());
            existing.setNoticeDate(notice.getNoticeDate());
            existing.setAttachmentUrl(notice.getAttachmentUrl());
            existing.setActive(notice.isActive());
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return repo.findById(id).map(n -> {
            repo.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

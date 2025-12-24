package com.smartvillage.feedback.controller;

import com.smartvillage.feedback.entity.Feedback;
import com.smartvillage.feedback.repository.FeedbackRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {
    private final FeedbackRepository repo;

    public FeedbackController(FeedbackRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Feedback> list() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        Optional<Feedback> f = repo.findById(id);
        return f.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Feedback create(@RequestBody Feedback feedback) { return repo.save(feedback); }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Feedback feedback) {
        return repo.findById(id).map(existing -> {
            existing.setMessage(feedback.getMessage());
            existing.setStatus(feedback.getStatus());
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return repo.findById(id).map(f -> {
            repo.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

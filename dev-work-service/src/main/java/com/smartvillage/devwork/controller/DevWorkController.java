package com.smartvillage.devwork.controller;

import com.smartvillage.devwork.entity.DevWork;
import com.smartvillage.devwork.repository.DevWorkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devworks")
public class DevWorkController {
    private final DevWorkRepository repo;

    public DevWorkController(DevWorkRepository repo) { this.repo = repo; }

    @GetMapping
    public List<DevWork> list() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        Optional<DevWork> w = repo.findById(id);
        return w.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public DevWork create(@RequestBody DevWork work) { return repo.save(work); }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody DevWork work) {
        return repo.findById(id).map(existing -> {
            existing.setTitle(work.getTitle());
            existing.setDescription(work.getDescription());
            existing.setStatus(work.getStatus());
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return repo.findById(id).map(w -> {
            repo.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

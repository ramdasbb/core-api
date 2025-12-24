package com.smartvillage.services.controller;

import com.smartvillage.services.entity.ServiceEntry;
import com.smartvillage.services.repository.ServiceEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceEntryController {
    private final ServiceEntryRepository repo;

    public ServiceEntryController(ServiceEntryRepository repo) { this.repo = repo; }

    @GetMapping
    public List<ServiceEntry> list() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        Optional<ServiceEntry> e = repo.findById(id);
        return e.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ServiceEntry create(@RequestBody ServiceEntry entry) { return repo.save(entry); }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ServiceEntry entry) {
        return repo.findById(id).map(existing -> {
            existing.setName(entry.getName());
            existing.setDescription(entry.getDescription());
            existing.setCategory(entry.getCategory());
            existing.setContact(entry.getContact());
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return repo.findById(id).map(e -> {
            repo.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

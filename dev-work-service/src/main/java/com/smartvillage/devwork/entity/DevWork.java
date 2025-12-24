package com.smartvillage.devwork.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dev_works")
public class DevWork {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private java.util.UUID id;

    private String title;
    private String status; // planned, ongoing, completed
    private String description;
    private Instant createdAt = Instant.now();

    public DevWork() {}

    public java.util.UUID getId() { return id; }
    public void setId(java.util.UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

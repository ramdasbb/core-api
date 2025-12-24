package com.smartvillage.feedback.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feedback_submissions")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private java.util.UUID id;

    private java.util.UUID villageId;
    private String name;
    private String mobile;
    private String type; // feedback, suggestion, complaint
    @Column(columnDefinition = "text")
    private String message;
    private String status = "new";
    private Instant createdAt = Instant.now();

    public Feedback() {}

    public java.util.UUID getId() { return id; }
    public void setId(java.util.UUID id) { this.id = id; }
    public java.util.UUID getVillageId() { return villageId; }
    public void setVillageId(java.util.UUID villageId) { this.villageId = villageId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

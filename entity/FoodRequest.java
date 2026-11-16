package com.AnnSavak.AnnSavak.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_requests")
public class FoodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Column(name = "recipient_latitude")
    private Double recipientLatitude;

    @Column(name = "recipient_longitude")
    private Double recipientLongitude;

    @ManyToOne
    @JoinColumn(name = "ngo_id", nullable = false)
    private User ngo;

    private String status;

    private LocalDateTime createdAt;

    public FoodRequest() {
    }

    public FoodRequest(Food food, User ngo, String status) {
        this.food = food;
        this.ngo = ngo;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public User getNgo() {
        return ngo;
    }

    public void setNgo(User ngo) {
        this.ngo = ngo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getRecipientLatitude() {
        return recipientLatitude;
    }

    public void setRecipientLatitude(Double recipientLatitude) {
        this.recipientLatitude = recipientLatitude;
    }

    public Double getRecipientLongitude() {
        return recipientLongitude;
    }

    public void setRecipientLongitude(Double recipientLongitude) {
        this.recipientLongitude = recipientLongitude;
    }
}
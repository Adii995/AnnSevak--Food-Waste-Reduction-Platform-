
package com.AnnSavak.AnnSavak.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String foodName;

    private Double quantity;

    // Intended purpose shown on the donor dashboard
    // Human Consumption, Animal Feed, Food Recovery / Waste Management
    private String foodType;

    // Dietary classification: VEG, NON_VEG, VEGAN
    private String dietaryType;

    // Internal purpose category
    // HUMAN_CONSUMPTION, ANIMAL_FEED, WASTE_MANAGEMENT
    private String foodCategory;

    // Only used for WASTE_MANAGEMENT
    // COMPOSTING or WASTE_DISPOSAL
    private String wasteManagementType;

    private LocalDate preparedDate;

    private LocalDate expiryDate;

    private String pickupLocation;

    private String image;

    private String status;

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private User restaurant;

    public Food() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public String getDietaryType() {
        return dietaryType;
    }

    public void setDietaryType(String dietaryType) {
        this.dietaryType = dietaryType;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public String getWasteManagementType() {
        return wasteManagementType;
    }

    public void setWasteManagementType(String wasteManagementType) {
        this.wasteManagementType = wasteManagementType;
    }

    public LocalDate getPreparedDate() {
        return preparedDate;
    }

    public void setPreparedDate(LocalDate preparedDate) {
        this.preparedDate = preparedDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(User restaurant) {
        this.restaurant = restaurant;
    }
}

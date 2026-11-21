package com.AnnSavak.AnnSavak.repository;

import com.AnnSavak.AnnSavak.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findByRestaurantId(Long restaurantId);

    List<Food> findByStatusAndExpiryDateGreaterThanEqualOrderByExpiryDateAsc(
            String status,
            LocalDate date
    );
    List<Food> findByRestaurantIdAndStatus(Long restaurantId, String status);
}
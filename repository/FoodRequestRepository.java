package com.AnnSavak.AnnSavak.repository;

import com.AnnSavak.AnnSavak.entity.FoodRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRequestRepository
        extends JpaRepository<FoodRequest, Long> {

    List<FoodRequest> findByFoodRestaurantIdOrderByCreatedAtDesc(
            Long restaurantId
    );

    List<FoodRequest> findByNgoIdOrderByCreatedAtDesc(
            Long ngoId
    );

    boolean existsByFoodIdAndStatus(
            Long foodId,
            String status
    );
    boolean existsByFoodId(Long foodId);

}
package com.AnnSavak.AnnSavak.controller;

import com.AnnSavak.AnnSavak.entity.Food;
import com.AnnSavak.AnnSavak.entity.FoodRequest;
import com.AnnSavak.AnnSavak.entity.User;
import com.AnnSavak.AnnSavak.repository.FoodRepository;
import com.AnnSavak.AnnSavak.repository.FoodRequestRepository;
import com.AnnSavak.AnnSavak.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDate;
import java.util.List;

@Controller
public class FoodRequestController {

    private final FoodRepository foodRepository;
    private final FoodRequestRepository foodRequestRepository;
    private final UserRepository userRepository;

    public FoodRequestController(
            FoodRepository foodRepository,
            FoodRequestRepository foodRequestRepository,
            UserRepository userRepository) {

        this.foodRepository = foodRepository;
        this.foodRequestRepository = foodRequestRepository;
        this.userRepository = userRepository;
    }

    private String normalizeCategory(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
    }

    private boolean isEligibleFood(Food food, String recipientCategory) {
        String category = normalizeCategory(recipientCategory);
        String purpose = normalizeCategory(food.getFoodCategory());

        if (purpose.isEmpty()) {
            purpose = normalizeCategory(food.getFoodType());
        }

        if (purpose.equals("HUMANCONSUMPTION")) {
            return Set.of("NGO", "OLDAGEHOME", "CHILDCAREHOME")
                    .contains(category);
        }

        if (purpose.equals("ANIMALFEED")) {
            return Set.of("SHELTERHOME", "GAUSHALA")
                    .contains(category);
        }

        if (purpose.equals("WASTEMANAGEMENT")
                || purpose.equals("FOODRECOVERYWASTEMANAGEMENT")) {
            return Set.of("COMPOSTINGPARTNER", "WASTEMANAGEMENTPARTNER")
                    .contains(category);
        }

        return false;
    }

    // NGO - AVAILABLE FOOD
    @GetMapping("/ngo/available-food")
    public String availableFood(Model model) {

        List<Food> foods =
                foodRepository
                        .findByStatusAndExpiryDateGreaterThanEqualOrderByExpiryDateAsc(
                                "AVAILABLE",
                                LocalDate.now()
                        );

        model.addAttribute("foods", foods);
        return "ngo-food";
    }

    // NGO / RECIPIENT - REQUEST FOOD
    @PostMapping({
            "/ngo/request-food/{foodId}",
            "/recipient/request-food/{foodId}"
    })
    public String requestFood(
            @PathVariable Long foodId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            Authentication authentication) {

        User recipient = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Recipient user not found"));

        if (!"RECIPIENT".equals(recipient.getAccountType())) {
            return "redirect:/";
        }

        String redirectUrl = "/recipient/dashboard";

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() ->
                        new RuntimeException("Food not found"));

        if (!isEligibleFood(food, recipient.getCategory())) {
            return "redirect:" + redirectUrl + "?error=ineligible";
        }

        if (!"AVAILABLE".equals(food.getStatus())) {
            return "redirect:" + redirectUrl + "?error=unavailable";
        }

        if (food.getExpiryDate() == null
                || food.getExpiryDate().isBefore(LocalDate.now())) {
            return "redirect:" + redirectUrl + "?error=expired";
        }

        if (latitude == null || longitude == null
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            return "redirect:" + redirectUrl + "?error=location";
        }

        boolean alreadyRequested =
                foodRequestRepository.existsByFoodIdAndStatus(
                        foodId, "PENDING");

        if (alreadyRequested) {
            return "redirect:" + redirectUrl + "?error=requested";
        }

        FoodRequest request = new FoodRequest();
        request.setFood(food);
        request.setNgo(recipient);
        request.setStatus("PENDING");
        request.setRecipientLatitude(latitude);
        request.setRecipientLongitude(longitude);

        foodRequestRepository.save(request);

        food.setStatus("REQUESTED");
        foodRepository.save(food);

        return "redirect:" + redirectUrl + "?success=requested";
    }

    // NGO - MY REQUESTS
    @GetMapping("/ngo/my-requests")
    public String myRequests(
            Authentication authentication,
            Model model) {

        User ngo = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("NGO user not found"));

        List<FoodRequest> requests =
                foodRequestRepository
                        .findByNgoIdOrderByCreatedAtDesc(ngo.getId());

        model.addAttribute("requests", requests);
        return "ngo-requests";
    }

    // RESTAURANT / DONOR - PICKUP REQUESTS
    @GetMapping({
            "/restaurant/pickup-requests",
            "/donor/pickup-requests"
    })
    public String restaurantPickupRequests(
            Authentication authentication,
            Model model) {

        User restaurant = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Restaurant user not found"));

        List<FoodRequest> requests =
                foodRequestRepository
                        .findByFoodRestaurantIdOrderByCreatedAtDesc(
                                restaurant.getId()
                        );

        model.addAttribute("requests", requests);
        model.addAttribute(
                "isDonorDashboard",
                "DONOR".equals(restaurant.getAccountType())
        );

        return "restaurant-pickup-requests";
    }

    // RESTAURANT / DONOR - ACCEPT REQUEST
    @PostMapping({
            "/restaurant/pickup-requests/{requestId}/accept",
            "/donor/pickup-requests/{requestId}/accept"
    })
    public String acceptRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        User restaurant = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Restaurant user not found"));

        FoodRequest request = foodRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Request not found"));

        if (!request.getFood()
                .getRestaurant()
                .getId()
                .equals(restaurant.getId())) {

            return "redirect:" +
                    ("DONOR".equals(restaurant.getAccountType())
                            ? "/donor/pickup-requests"
                            : "/restaurant/pickup-requests");
        }

        if (!"PENDING".equals(request.getStatus())) {
            return "redirect:" +
                    ("DONOR".equals(restaurant.getAccountType())
                            ? "/donor/pickup-requests"
                            : "/restaurant/pickup-requests");
        }

        request.setStatus("ACCEPTED");
        foodRequestRepository.save(request);

        request.getFood().setStatus("ACCEPTED");
        foodRepository.save(request.getFood());

        return "redirect:" +
                ("DONOR".equals(restaurant.getAccountType())
                        ? "/donor/pickup-requests"
                        : "/restaurant/pickup-requests");
    }

    // RESTAURANT / DONOR - REJECT REQUEST
    @PostMapping({
            "/restaurant/pickup-requests/{requestId}/reject",
            "/donor/pickup-requests/{requestId}/reject"
    })
    public String rejectRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        User restaurant = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Restaurant user not found"));

        FoodRequest request = foodRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Request not found"));

        if (!request.getFood()
                .getRestaurant()
                .getId()
                .equals(restaurant.getId())) {

            return "redirect:" +
                    ("DONOR".equals(restaurant.getAccountType())
                            ? "/donor/pickup-requests"
                            : "/restaurant/pickup-requests");
        }


        Food food = request.getFood();

        request.setStatus("REJECTED");
        foodRequestRepository.save(request);

        if (food.getExpiryDate() == null
                || food.getExpiryDate().isBefore(LocalDate.now())) {
            food.setStatus("EXPIRED");
        } else {
            food.setStatus("AVAILABLE");
        }

        foodRepository.save(food);

        return "redirect:" +
                ("DONOR".equals(restaurant.getAccountType())
                        ? "/donor/pickup-requests"
                        : "/restaurant/pickup-requests");
    }

    // RESTAURANT / DONOR - MARK COLLECTED
    @PostMapping({
            "/restaurant/pickup-requests/{requestId}/collected",
            "/donor/pickup-requests/{requestId}/collected"
    })
    public String markCollected(
            @PathVariable Long requestId,
            Authentication authentication) {

        User restaurant = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Restaurant user not found"));

        FoodRequest request = foodRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Request not found"));

        if (!request.getFood()
                .getRestaurant()
                .getId()
                .equals(restaurant.getId())) {

            return "redirect:" +
                    ("DONOR".equals(restaurant.getAccountType())
                            ? "/donor/pickup-requests"
                            : "/restaurant/pickup-requests");
        }

        if (!"ACCEPTED".equals(request.getStatus())) {
            return "redirect:" +
                    ("DONOR".equals(restaurant.getAccountType())
                            ? "/donor/pickup-requests"
                            : "/restaurant/pickup-requests");
        }

        request.setStatus("COLLECTED");
        foodRequestRepository.save(request);

        request.getFood().setStatus("COLLECTED");
        foodRepository.save(request.getFood());

        return "redirect:" +
                ("DONOR".equals(restaurant.getAccountType())
                        ? "/donor/pickup-requests"
                        : "/restaurant/pickup-requests");
    }
}
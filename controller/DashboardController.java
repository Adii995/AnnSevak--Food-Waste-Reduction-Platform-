
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
public class DashboardController {

    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final FoodRequestRepository foodRequestRepository;

    public DashboardController(
            FoodRepository foodRepository,
            UserRepository userRepository,
            FoodRequestRepository foodRequestRepository) {
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.foodRequestRepository = foodRequestRepository;
    }

    // RESTAURANT DASHBOARD
    @GetMapping("/restaurant/dashboard")
    public String restaurantDashboard(
            Authentication authentication,
            Model model) {

        User restaurant = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Restaurant user not found"));

        List<Food> foods =
                foodRepository.findByRestaurantIdAndStatus(
                        restaurant.getId(), "AVAILABLE");

        model.addAttribute("foods", foods);
        model.addAttribute("restaurant", restaurant);

        return "restaurant-dashboard";
    }

    // DONOR DASHBOARD
    @GetMapping("/donor/dashboard")
    public String donorDashboard(
            Authentication authentication,
            Model model) {

        User donor = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Donor user not found"));

        if (!"DONOR".equals(donor.getAccountType())) {
            return "redirect:/";
        }

        List<Food> foods =
                foodRepository.findByRestaurantIdAndStatus(
                        donor.getId(), "AVAILABLE");

        model.addAttribute("donor", donor);
        model.addAttribute("foods", foods);
        model.addAttribute("category", donor.getCategory());

        return "donor-dashboard";
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

        // Older records may only have foodType populated.
        if (purpose.isEmpty()) {
            purpose = normalizeCategory(food.getFoodType());
        }

        Set<String> humanRecipients = Set.of(
                "NGO", "OLDAGEHOME", "CHILDCAREHOME"
        );

        Set<String> animalRecipients = Set.of(
                "SHELTERHOME", "GAUSHALA"
        );

        Set<String> wasteRecipients = Set.of(
                "COMPOSTINGPARTNER", "WASTEMANAGEMENTPARTNER"
        );

        if (purpose.equals("HUMANCONSUMPTION")) {
            return humanRecipients.contains(category);
        }

        if (purpose.equals("ANIMALFEED")) {
            return animalRecipients.contains(category);
        }

        if (purpose.equals("WASTEMANAGEMENT")
                || purpose.equals("FOODRECOVERYWASTEMANAGEMENT")) {
            return wasteRecipients.contains(category);
        }

        return false;
    }

    // DEFAULT RADIUS: HUMAN 20 KM, ANIMAL 20 KM, WASTE 30 KM
    private int getDefaultRadius(String recipientCategory) {
        String category = normalizeCategory(recipientCategory);

        if (Set.of("COMPOSTINGPARTNER", "WASTEMANAGEMENTPARTNER")
                .contains(category)) {
            return 30;
        }

        return 20;
    }

    // DISTANCE BETWEEN TWO COORDINATES IN KILOMETRES
    private double calculateDistanceKm(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {

        double earthRadiusKm = 6371.0;

        double latDistance = Math.toRadians(latitude2 - latitude1);
        double lonDistance = Math.toRadians(longitude2 - longitude1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    // RECIPIENT DASHBOARD WITH NEAREST FOOD FILTER
    @GetMapping("/recipient/dashboard")
    public String recipientDashboard(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) Integer radius) {

        User recipient = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Recipient user not found"));

        if (!"RECIPIENT".equals(recipient.getAccountType())) {
            return "redirect:/";
        }

        int defaultRadius = getDefaultRadius(recipient.getCategory());

        // Only allow the five supported radius choices.
        Set<Integer> allowedRadii = Set.of(10, 20, 30, 40, 50);

        int selectedRadius =
                radius != null && allowedRadii.contains(radius)
                        ? radius
                        : defaultRadius;

        List<Food> allAvailableFoods = foodRepository
                .findByStatusAndExpiryDateGreaterThanEqualOrderByExpiryDateAsc(
                        "AVAILABLE", LocalDate.now());

        Map<Long, Double> foodDistances = new HashMap<>();

        List<Food> availableFoods = allAvailableFoods.stream()
                .filter(food -> isEligibleFood(food, recipient.getCategory()))
                .filter(food -> {
                    User donor = food.getRestaurant();

                    // Distance cannot be determined without both locations.
                    if (donor == null
                            || recipient.getLatitude() == null
                            || recipient.getLongitude() == null
                            || donor.getLatitude() == null
                            || donor.getLongitude() == null) {
                        return false;
                    }

                    double distance = calculateDistanceKm(
                            recipient.getLatitude(),
                            recipient.getLongitude(),
                            donor.getLatitude(),
                            donor.getLongitude()
                    );

                    if (distance <= selectedRadius) {
                        foodDistances.put(food.getId(), distance);
                        return true;
                    }

                    return false;
                })
                .sorted(Comparator.comparingDouble(
                        food -> foodDistances.get(food.getId())))
                .toList();

        List<FoodRequest> myRequests =
                foodRequestRepository.findByNgoIdOrderByCreatedAtDesc(
                        recipient.getId());

        long availableFoodCount = availableFoods.size();
        long myRequestCount = myRequests.size();

        long acceptedCount = myRequests.stream()
                .filter(request -> "ACCEPTED".equals(request.getStatus()))
                .count();

        long collectedCount = myRequests.stream()
                .filter(request -> "COLLECTED".equals(request.getStatus()))
                .count();

        double totalAvailableQuantity = availableFoods.stream()
                .mapToDouble(food ->
                        food.getQuantity() != null ? food.getQuantity() : 0)
                .sum();

        double totalFoodReceived = myRequests.stream()
                .filter(request -> "COLLECTED".equals(request.getStatus()))
                .mapToDouble(request ->
                        request.getFood().getQuantity() != null
                                ? request.getFood().getQuantity() : 0)
                .sum();

        model.addAttribute("recipient", recipient);
        model.addAttribute("foods", availableFoods);
        model.addAttribute("foodDistances", foodDistances);
        model.addAttribute("requests", myRequests);
        model.addAttribute("category", recipient.getCategory());
        model.addAttribute("availableFoodCount", availableFoodCount);
        model.addAttribute("myRequestCount", myRequestCount);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("collectedCount", collectedCount);
        model.addAttribute("totalAvailableQuantity", totalAvailableQuantity);
        model.addAttribute("totalFoodReceived", totalFoodReceived);
        model.addAttribute("selectedRadius", selectedRadius);
        model.addAttribute("defaultRadius", defaultRadius);

        return "recipient-dashboard";
    }

    // NGO DASHBOARD — EXISTING BEHAVIOUR PRESERVED
    @GetMapping("/ngo/dashboard")
    public String ngoDashboard(
            Authentication authentication,
            Model model) {

        User ngo = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("NGO user not found"));

        List<Food> availableFoods = foodRepository
                .findByStatusAndExpiryDateGreaterThanEqualOrderByExpiryDateAsc(
                        "AVAILABLE", LocalDate.now());

        List<FoodRequest> myRequests =
                foodRequestRepository.findByNgoIdOrderByCreatedAtDesc(
                        ngo.getId());

        long availableFoodCount = availableFoods.size();

        long myRequestCount = myRequests.size();

        long acceptedCount = myRequests.stream()
                .filter(request -> "ACCEPTED".equals(request.getStatus()))
                .count();

        long collectedCount = myRequests.stream()
                .filter(request -> "COLLECTED".equals(request.getStatus()))
                .count();

        double totalAvailableQuantity = availableFoods.stream()
                .mapToDouble(food ->
                        food.getQuantity() != null ? food.getQuantity() : 0)
                .sum();

        double totalFoodReceived = myRequests.stream()
                .filter(request -> "COLLECTED".equals(request.getStatus()))
                .mapToDouble(request ->
                        request.getFood().getQuantity() != null
                                ? request.getFood().getQuantity() : 0)
                .sum();

        model.addAttribute("ngo", ngo);
        model.addAttribute("foods", availableFoods);
        model.addAttribute("requests", myRequests);
        model.addAttribute("availableFoodCount", availableFoodCount);
        model.addAttribute("myRequestCount", myRequestCount);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("collectedCount", collectedCount);
        model.addAttribute("totalAvailableQuantity", totalAvailableQuantity);
        model.addAttribute("totalFoodReceived", totalFoodReceived);

        return "ngo-dashboard";
    }
}

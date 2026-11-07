
package com.AnnSavak.AnnSavak.controller;

import com.AnnSavak.AnnSavak.entity.Food;
import com.AnnSavak.AnnSavak.entity.User;
import com.AnnSavak.AnnSavak.repository.FoodRepository;
import com.AnnSavak.AnnSavak.repository.UserRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import com.AnnSavak.AnnSavak.repository.FoodRequestRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Controller
public class FoodController {

    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final FoodRequestRepository foodRequestRepository;

    public FoodController(
            FoodRepository foodRepository,
            UserRepository userRepository,
            FoodRequestRepository foodRequestRepository) {

        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.foodRequestRepository = foodRequestRepository;
    }

    @GetMapping("/restaurant/add-food")
    public String showAddFoodPage() {
        return "add-food";
    }

    @GetMapping("/donor/add-food")
    public String showDonorAddFoodPage() {
        return "add-food";
    }

    @PostMapping({"/restaurant/add-food", "/donor/add-food"})
    public String addFood(
            @RequestParam("foodName") String foodName,
            @RequestParam("quantity") Double quantity,

            // Intended purpose, displayed on dashboard
            @RequestParam("foodType") String foodType,

            // Dietary classification
            @RequestParam("dietaryType") String dietaryType,

            // Waste management option, if applicable
            @RequestParam(value = "wasteManagementType", required = false)
            String wasteManagementType,

            @RequestParam("preparedDate") LocalDate preparedDate,
            @RequestParam("expiryDate") LocalDate expiryDate,
            @RequestParam("image") MultipartFile imageFile,
            Authentication authentication
    ) throws IOException {

        // Validate food name and quantity
        if (foodName == null || foodName.isBlank()) {
            throw new IllegalArgumentException("Food name is required.");
        }

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        // Validate dietary type
        if (!"VEG".equals(dietaryType)
                && !"NON_VEG".equals(dietaryType)
                && !"VEGAN".equals(dietaryType)) {
            throw new IllegalArgumentException("Invalid dietary type.");
        }

        // Map the visible purpose to the internal category
        String foodCategory;

        if ("Human Consumption".equals(foodType)) {
            foodCategory = "HUMAN_CONSUMPTION";

        } else if ("Animal Feed".equals(foodType)) {
            foodCategory = "ANIMAL_FEED";

        } else if ("Food Recovery / Waste Management".equals(foodType)) {
            foodCategory = "WASTE_MANAGEMENT";

        } else {
            throw new IllegalArgumentException("Invalid food purpose.");
        }

        // Validate waste-management subcategory
        if ("WASTE_MANAGEMENT".equals(foodCategory)) {
            if (!"COMPOSTING".equals(wasteManagementType)
                    && !"WASTE_DISPOSAL".equals(wasteManagementType)) {
                throw new IllegalArgumentException(
                        "Please select a valid waste management option."
                );
            }
        } else {
            wasteManagementType = null;
        }

        // Validate dates
        if (preparedDate == null || expiryDate == null) {
            throw new IllegalArgumentException(
                    "Prepared date and use-before date are required."
            );
        }

        if (expiryDate.isBefore(preparedDate)) {
            throw new IllegalArgumentException(
                    "Expiry date cannot be earlier than prepared date."
            );
        }

        // Validate image
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please upload a food photo."
            );
        }

        String contentType = imageFile.getContentType();

        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)) {
            throw new IllegalArgumentException(
                    "Only JPG and PNG images are allowed."
            );
        }

        // Find logged-in user
        User restaurant = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found.")
                );

        // Create food object
        Food food = new Food();

        food.setFoodName(foodName.trim());
        food.setQuantity(quantity);

        // Save the exact display value expected by dashboard badges
        food.setFoodType(foodType);

        food.setDietaryType(dietaryType);
        food.setFoodCategory(foodCategory);
        food.setWasteManagementType(wasteManagementType);
        food.setPreparedDate(preparedDate);
        food.setExpiryDate(expiryDate);

        // Build pickup location from registered address
        String savedAddress = restaurant.getAddress();
        String savedLocation = restaurant.getLocation();

        StringBuilder pickupLocation = new StringBuilder();

        if (savedAddress != null && !savedAddress.isBlank()) {
            pickupLocation.append(savedAddress.trim());
        }

        if (savedLocation != null && !savedLocation.isBlank()) {
            if (!pickupLocation.isEmpty()) {
                pickupLocation.append(", ");
            }

            pickupLocation.append(savedLocation.trim());
        }

        if (pickupLocation.isEmpty()) {
            throw new IllegalStateException(
                    "Please update your registered pickup address "
                            + "before adding food."
            );
        }

        food.setPickupLocation(pickupLocation.toString());

        // Associate food with logged-in donor
        food.setRestaurant(restaurant);

        // Initial status
        food.setStatus("AVAILABLE");

        // Save uploaded image
        Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        // Generate filename rather than trusting the original filename
        String extension = "image/png".equals(contentType)
                ? ".png"
                : ".jpg";

        String fileName = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(fileName).normalize();

        // Ensure target remains inside the uploads directory
        if (!filePath.startsWith(uploadPath)) {
            throw new IOException("Invalid upload path.");
        }

        Files.copy(imageFile.getInputStream(), filePath);
        food.setImage(fileName);

        // Save food to database
        foodRepository.save(food);

        // Redirect based on the user's account type
        return "DONOR".equals(restaurant.getAccountType())
                ? "redirect:/donor/dashboard"
                : "redirect:/restaurant/dashboard";
    }
    @PostMapping("/donor/delete-expired-food/{id}")
    public String deleteExpiredFood(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User donor = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found.")
                );

        if (!"DONOR".equals(donor.getAccountType())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only donors can delete their food."
            );
            return "redirect:/donor/dashboard";
        }

        Food food = foodRepository.findById(id)
                .orElse(null);

        if (food == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Food listing not found."
            );
            return "redirect:/donor/dashboard";
        }

        if (food.getRestaurant() == null
                || !food.getRestaurant().getId().equals(donor.getId())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You can delete only your own food."
            );
            return "redirect:/donor/dashboard";
        }

        if (food.getExpiryDate() == null
                || !food.getExpiryDate().isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only expired food can be deleted."
            );
            return "redirect:/donor/dashboard";
        }
        if (foodRequestRepository.existsByFoodId(id)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This food has linked pickup requests and cannot be deleted."
            );
            return "redirect:/donor/dashboard";
        }
        foodRepository.delete(food);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Expired food deleted successfully."
        );

        return "redirect:/donor/dashboard";
    }
}

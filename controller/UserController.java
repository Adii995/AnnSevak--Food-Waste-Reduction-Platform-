package com.AnnSavak.AnnSavak.controller;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.AnnSavak.AnnSavak.entity.User;
import com.AnnSavak.AnnSavak.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }
    @PostMapping("/forgot-password")
    public String forgotPassword(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No account found with this email address."
            );

            return "redirect:/forgot-password";
        }

        String resetToken = UUID.randomUUID().toString();

        user.setResetToken(resetToken);
        user.setResetTokenExpiry(
                java.time.LocalDateTime.now().plusMinutes(15)
        );

        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "success",
                "Password reset request created successfully."
        );

        redirectAttributes.addFlashAttribute(
                "resetLink",
                "/reset-password?token=" + resetToken
        );

        return "redirect:/forgot-password";
    }
    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam("token") String token,
            Model model) {

        User user = userRepository.findAll()
                .stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst()
                .orElse(null);

        if (user == null ||
                user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {

            return "redirect:/forgot-password?error=invalid";
        }

        model.addAttribute("token", token);

        return "reset-password";
    }
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findAll()
                .stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst()
                .orElse(null);

        if (user == null ||
                user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "This password reset link is invalid or expired."
            );

            return "redirect:/forgot-password";
        }

        if (!password.equals(confirmPassword)) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Passwords do not match."
            );

            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 8) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Password must be at least 8 characters."
            );

            return "redirect:/reset-password?token=" + token;
        }

        user.setPassword(passwordEncoder.encode(password));

        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);



        redirectAttributes.addFlashAttribute(
                "success",
                "Password reset successfully. You can now login."
        );

        return "redirect:/login?resetSuccess=true";
    }



    @PostMapping("/register")
    public String registerUser(
            @RequestParam("role") String role,

            @RequestParam(value = "restaurantName", required = false) String restaurantName,
            @RequestParam(value = "restaurantOwner", required = false) String restaurantOwner,
            @RequestParam(value = "restaurantAddress", required = false) String restaurantAddress,
            @RequestParam(value = "restaurantLocation", required = false) String restaurantLocation,
            @RequestParam(value = "restaurantFssai", required = false) String restaurantFssai,
            @RequestParam(value = "restaurantGstin", required = false) String restaurantGstin,
            @RequestParam(value = "restaurantMobile", required = false) String restaurantMobile,
            @RequestParam(value = "restaurantEmail", required = false) String restaurantEmail,
            @RequestParam(value = "restaurantPassword", required = false) String restaurantPassword,

            @RequestParam(value = "hotelName", required = false) String hotelName,
            @RequestParam(value = "hotelOwner", required = false) String hotelOwner,
            @RequestParam(value = "hotelAddress", required = false) String hotelAddress,
            @RequestParam(value = "hotelLocation", required = false) String hotelLocation,
            @RequestParam(value = "hotelFssai", required = false) String hotelFssai,
            @RequestParam(value = "hotelGstin", required = false) String hotelGstin,
            @RequestParam(value = "hotelMobile", required = false) String hotelMobile,
            @RequestParam(value = "hotelEmail", required = false) String hotelEmail,
            @RequestParam(value = "hotelPassword", required = false) String hotelPassword,

            @RequestParam(value = "banquetName", required = false) String banquetName,
            @RequestParam(value = "banquetOwner", required = false) String banquetOwner,
            @RequestParam(value = "banquetAddress", required = false) String banquetAddress,
            @RequestParam(value = "banquetLocation", required = false) String banquetLocation,
            @RequestParam(value = "banquetMobile", required = false) String banquetMobile,
            @RequestParam(value = "banquetEmail", required = false) String banquetEmail,
            @RequestParam(value = "banquetPassword", required = false) String banquetPassword,

            @RequestParam(value = "cateringName", required = false) String cateringName,
            @RequestParam(value = "cateringOwner", required = false) String cateringOwner,
            @RequestParam(value = "cateringAddress", required = false) String cateringAddress,
            @RequestParam(value = "cateringLocation", required = false) String cateringLocation,
            @RequestParam(value = "cateringFssai", required = false) String cateringFssai,
            @RequestParam(value = "cateringGstin", required = false) String cateringGstin,
            @RequestParam(value = "cateringMobile", required = false) String cateringMobile,
            @RequestParam(value = "cateringEmail", required = false) String cateringEmail,
            @RequestParam(value = "cateringPassword", required = false) String cateringPassword,

            @RequestParam(value = "hospitalName", required = false) String hospitalName,
            @RequestParam(value = "hospitalAuthority", required = false) String hospitalAuthority,
            @RequestParam(value = "hospitalAddress", required = false) String hospitalAddress,
            @RequestParam(value = "hospitalLocation", required = false) String hospitalLocation,
            @RequestParam(value = "hospitalMobile", required = false) String hospitalMobile,
            @RequestParam(value = "hospitalEmail", required = false) String hospitalEmail,
            @RequestParam(value = "hospitalPassword", required = false) String hospitalPassword,

            @RequestParam(value = "eventName", required = false) String eventName,
            @RequestParam(value = "eventAuthority", required = false) String eventAuthority,
            @RequestParam(value = "eventAddress", required = false) String eventAddress,
            @RequestParam(value = "eventLocation", required = false) String eventLocation,
            @RequestParam(value = "eventMobile", required = false) String eventMobile,
            @RequestParam(value = "eventEmail", required = false) String eventEmail,
            @RequestParam(value = "eventPassword", required = false) String eventPassword,

            @RequestParam(value = "ngoName", required = false) String ngoName,
            @RequestParam(value = "ngoAddress", required = false) String ngoAddress,
            @RequestParam(value = "ngoMobile", required = false) String ngoMobile,
            @RequestParam(value = "ngoEmail", required = false) String ngoEmail,
            @RequestParam(value = "ngoPassword", required = false) String ngoPassword,

            @RequestParam(value = "oldAgeHomeName", required = false) String oldAgeHomeName,
            @RequestParam(value = "oldAgeHomeAddress", required = false) String oldAgeHomeAddress,
            @RequestParam(value = "oldAgeHomeMobile", required = false) String oldAgeHomeMobile,
            @RequestParam(value = "oldAgeHomeEmail", required = false) String oldAgeHomeEmail,
            @RequestParam(value = "oldAgeHomePassword", required = false) String oldAgeHomePassword,

            @RequestParam(value = "childCareName", required = false) String childCareName,
            @RequestParam(value = "childCareAddress", required = false) String childCareAddress,
            @RequestParam(value = "childCareMobile", required = false) String childCareMobile,
            @RequestParam(value = "childCareEmail", required = false) String childCareEmail,
            @RequestParam(value = "childCarePassword", required = false) String childCarePassword,

            @RequestParam(value = "shelterName", required = false) String shelterName,
            @RequestParam(value = "shelterAddress", required = false) String shelterAddress,
            @RequestParam(value = "shelterMobile", required = false) String shelterMobile,
            @RequestParam(value = "shelterEmail", required = false) String shelterEmail,
            @RequestParam(value = "shelterPassword", required = false) String shelterPassword,

            @RequestParam(value = "compostingName", required = false) String compostingName,
            @RequestParam(value = "compostingAddress", required = false) String compostingAddress,
            @RequestParam(value = "compostingMobile", required = false) String compostingMobile,
            @RequestParam(value = "compostingEmail", required = false) String compostingEmail,
            @RequestParam(value = "compostingPassword", required = false) String compostingPassword,

            @RequestParam(value = "wasteName", required = false) String wasteName,
            @RequestParam(value = "wasteAddress", required = false) String wasteAddress,
            @RequestParam(value = "wasteMobile", required = false) String wasteMobile,
            @RequestParam(value = "wasteEmail", required = false) String wasteEmail,
            @RequestParam(value = "wastePassword", required = false) String wastePassword,

            @RequestParam(value = "gaushalaName", required = false) String gaushalaName,
            @RequestParam(value = "gaushalaResponsible", required = false) String gaushalaResponsible,
            @RequestParam(value = "gaushalaAddress", required = false) String gaushalaAddress,
            @RequestParam(value = "gaushalaLocation", required = false) String gaushalaLocation,
            @RequestParam(value = "gaushalaMobile", required = false) String gaushalaMobile,
            @RequestParam(value = "gaushalaEmail", required = false) String gaushalaEmail,
            @RequestParam(value = "gaushalaPassword", required = false) String gaushalaPassword,

            @RequestParam(value = "banquetOwnerPhoto", required = false) MultipartFile banquetOwnerPhoto,

            @RequestParam(value = "eventAuthorityPhoto", required = false) MultipartFile eventAuthorityPhoto,

            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude

    ) throws IOException {




        if (latitude == null || longitude == null ||
                !Double.isFinite(latitude) || !Double.isFinite(longitude) ||
                latitude < -90 || latitude > 90 ||
                longitude < -180 || longitude > 180) {
            return "redirect:/register?locationRequired=true";
        }


        User user = new User();

        user.setRole(role);
        user.setStatus("PENDING");
        user.setAccountStatus("PENDING");

        switch (role) {

            case "RESTAURANT":
                user.setAccountType("DONOR");
                user.setCategory("RESTAURANT");
                user.setBusinessName(restaurantName);
                user.setOwnerName(restaurantOwner);
                user.setAddress(restaurantAddress);
                user.setLocation(restaurantLocation);
                user.setFssai(restaurantFssai);
                user.setGstin(restaurantGstin);
                user.setMobile(restaurantMobile);
                user.setEmail(restaurantEmail);
                user.setPassword(passwordEncoder.encode(restaurantPassword));
                break;

            case "HOTEL":
                user.setAccountType("DONOR");
                user.setCategory("HOTEL");
                user.setBusinessName(hotelName);
                user.setOwnerName(hotelOwner);
                user.setAddress(hotelAddress);
                user.setLocation(hotelLocation);
                user.setFssai(hotelFssai);
                user.setGstin(hotelGstin);
                user.setMobile(hotelMobile);
                user.setEmail(hotelEmail);
                user.setPassword(passwordEncoder.encode(hotelPassword));
                break;

            case "BANQUET_HALL":
                user.setAccountType("DONOR");
                user.setCategory("BANQUET_HALL");
                user.setBusinessName(banquetName);
                user.setOwnerName(banquetOwner);
                user.setAddress(banquetAddress);
                user.setLocation(banquetLocation);
                user.setMobile(banquetMobile);
                user.setEmail(banquetEmail);
                user.setPassword(passwordEncoder.encode(banquetPassword));
                savePhoto(user, banquetOwnerPhoto);
                break;

            case "CATERING":
                user.setAccountType("DONOR");
                user.setCategory("CATERING");
                user.setBusinessName(cateringName);
                user.setOwnerName(cateringOwner);
                user.setAddress(cateringAddress);
                user.setLocation(cateringLocation);
                user.setFssai(cateringFssai);
                user.setGstin(cateringGstin);
                user.setMobile(cateringMobile);
                user.setEmail(cateringEmail);
                user.setPassword(passwordEncoder.encode(cateringPassword));
                break;

            case "HOSPITAL":
                user.setAccountType("DONOR");
                user.setCategory("HOSPITAL");
                user.setBusinessName(hospitalName);
                user.setOwnerName(hospitalAuthority);
                user.setAddress(hospitalAddress);
                user.setLocation(hospitalLocation);
                user.setMobile(hospitalMobile);
                user.setEmail(hospitalEmail);
                user.setPassword(passwordEncoder.encode(hospitalPassword));
                break;

            case "EVENT_ORGANIZER":
                user.setAccountType("DONOR");
                user.setCategory("EVENT_ORGANIZER");
                user.setBusinessName(eventName);
                user.setOwnerName(eventAuthority);
                user.setAddress(eventAddress);
                user.setLocation(eventLocation);
                user.setMobile(eventMobile);
                user.setEmail(eventEmail);
                user.setPassword(passwordEncoder.encode(eventPassword));
                savePhoto(user, eventAuthorityPhoto);
                break;

            case "NGO":
                user.setAccountType("RECIPIENT");
                user.setCategory("NGO");
                user.setBusinessName(ngoName);
                user.setAddress(ngoAddress);
                user.setMobile(ngoMobile);
                user.setEmail(ngoEmail);
                user.setPassword(passwordEncoder.encode(ngoPassword));
                break;

            case "OLD_AGE_HOME":
                user.setAccountType("RECIPIENT");
                user.setCategory("OLD_AGE_HOME");
                user.setBusinessName(oldAgeHomeName);
                user.setAddress(oldAgeHomeAddress);
                user.setMobile(oldAgeHomeMobile);
                user.setEmail(oldAgeHomeEmail);
                user.setPassword(passwordEncoder.encode(oldAgeHomePassword));
                break;

            case "CHILD_CARE_HOME":
                user.setAccountType("RECIPIENT");
                user.setCategory("CHILD_CARE_HOME");
                user.setBusinessName(childCareName);
                user.setAddress(childCareAddress);
                user.setMobile(childCareMobile);
                user.setEmail(childCareEmail);
                user.setPassword(passwordEncoder.encode(childCarePassword));
                break;

            case "SHELTER_HOME":
                user.setAccountType("RECIPIENT");
                user.setCategory("SHELTER_HOME");
                user.setBusinessName(shelterName);
                user.setAddress(shelterAddress);
                user.setMobile(shelterMobile);
                user.setEmail(shelterEmail);
                user.setPassword(passwordEncoder.encode(shelterPassword));
                break;

            case "COMPOSTING_PARTNER":
                user.setAccountType("RECIPIENT");
                user.setCategory("COMPOSTING_PARTNER");
                user.setBusinessName(compostingName);
                user.setAddress(compostingAddress);
                user.setMobile(compostingMobile);
                user.setEmail(compostingEmail);
                user.setPassword(passwordEncoder.encode(compostingPassword));
                break;

            case "WASTE_MANAGEMENT_PARTNER":
                user.setAccountType("RECIPIENT");
                user.setCategory("WASTE_MANAGEMENT_PARTNER");
                user.setBusinessName(wasteName);
                user.setAddress(wasteAddress);
                user.setMobile(wasteMobile);
                user.setEmail(wasteEmail);
                user.setPassword(passwordEncoder.encode(wastePassword));
                break;

            case "GAUSHALA":
                user.setAccountType("RECIPIENT");
                user.setCategory("GAUSHALA");
                user.setBusinessName(gaushalaName);
                user.setOwnerName(gaushalaResponsible);
                user.setAddress(gaushalaAddress);
                user.setLocation(gaushalaLocation);
                user.setMobile(gaushalaMobile);
                user.setEmail(gaushalaEmail);
                user.setPassword(passwordEncoder.encode(gaushalaPassword));
                break;

            default:
                return "redirect:/register";
        }


        user.setLatitude(latitude);
        user.setLongitude(longitude);

        user.setName(user.getBusinessName());

        userRepository.save(user);

        return "redirect:/login?registered=true";
    }

    private void savePhoto(User user, MultipartFile photo)
            throws IOException {

        if (photo == null || photo.isEmpty()) {
            return;
        }

        Path uploadPath = Paths.get("uploads");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = photo.getOriginalFilename();
        String extension = "";

        if (originalFileName != null &&
                originalFileName.contains(".")) {
            extension = originalFileName.substring(
                    originalFileName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(fileName);

        Files.copy(photo.getInputStream(), filePath);

        user.setOwnerPhoto(fileName);
    }
}
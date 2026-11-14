package com.AnnSavak.AnnSavak.controller;

import com.AnnSavak.AnnSavak.entity.User;
import com.AnnSavak.AnnSavak.repository.ComplaintRepository;
import com.AnnSavak.AnnSavak.repository.UserRepository;
import com.AnnSavak.AnnSavak.service.AdminTwoFactorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final AdminTwoFactorService twoFactorService;


    public AdminController(UserRepository userRepository,
                           AdminTwoFactorService twoFactorService,
                           PasswordEncoder passwordEncoder,ComplaintRepository complaintRepository) {
        this.userRepository = userRepository;
        this.twoFactorService = twoFactorService;
        this.complaintRepository=complaintRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/admin/2fa/setup")
    public String setupTwoFactor(Authentication authentication,
                                 Model model) {

        User admin = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.isTwoFactorEnabled()) {
            return "redirect:/admin/2fa/login";
        }

        String secret = admin.getTwoFactorSecret();

        if (secret == null || secret.isBlank()) {
            secret = twoFactorService.generateSecret();

            admin.setTwoFactorSecret(secret);
            userRepository.save(admin);
        }

        String qrCode = twoFactorService.generateQrCode(
                admin.getEmail(),
                secret
        );

        model.addAttribute("qrCode", qrCode);
        model.addAttribute("email", admin.getEmail());

        return "admin-2fa-setup";
    }

    @PostMapping("/admin/2fa/verify")
    public String verifyTwoFactor(
            Authentication authentication,
            @RequestParam("code") String code,
            HttpSession session) {

        User admin = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        boolean valid = twoFactorService.verifyCode(
                admin.getTwoFactorSecret(),
                code
        );

        if (!valid) {
            return "redirect:/admin/2fa/setup?error=true";
        }

        admin.setTwoFactorEnabled(true);
        userRepository.save(admin);

        session.setAttribute("ADMIN_2FA_VERIFIED", true);

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/2fa/login")
    public String adminTwoFactorLogin() {
        return "admin-2fa-login";
    }

    @PostMapping("/admin/2fa/login")
    public String verifyAdminLoginCode(
            Authentication authentication,
            @RequestParam("code") String code,
            HttpSession session) {

        User admin = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        boolean valid = twoFactorService.verifyCode(
                admin.getTwoFactorSecret(),
                code
        );

        if (!valid) {
            return "redirect:/admin/2fa/login?error=true";
        }

        session.setAttribute("ADMIN_2FA_VERIFIED", true);

        return "redirect:/admin/dashboard";
    }
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }
    @GetMapping("/admin/pending")
    public String pendingOrganizations(Model model) {
        model.addAttribute(
                "organizations",
                userRepository.findByStatus("PENDING")
        );
        return "admin-pending";
    }
    @GetMapping("/admin/approved")
    public String approvedOrganizations(Model model) {
        model.addAttribute(
                "organizations",
                userRepository.findByStatus("APPROVED")
        );

        return "admin-approved";
    }

    @PostMapping("/admin/organizations/status")
    public String updateOrganizationStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes) {

        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            redirectAttributes.addFlashAttribute(
                    "error", "Invalid organization status."
            );
            return "redirect:/admin/pending";
        }

        User organization = userRepository.findById(id)
                .orElse(null);

        if (organization == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Organization not found."
            );
            return "redirect:/admin/pending";
        }

        if (!"PENDING".equals(organization.getStatus())) {
            redirectAttributes.addFlashAttribute(
                    "error", "This organization has already been reviewed."
            );
            return "redirect:/admin/pending";
        }

        organization.setStatus(status);

        if ("APPROVED".equals(status)) {
            organization.setAccountStatus("ACTIVE");
        } else if ("REJECTED".equals(status)) {
            organization.setAccountStatus("SUSPENDED");
        }

        userRepository.save(organization);

        redirectAttributes.addFlashAttribute(
                "success",
                "APPROVED".equals(status)
                        ? "Organization approved successfully."
                        : "Organization rejected successfully."
        );

        return "redirect:/admin/pending";
    }

    @GetMapping("/admin/complaints")
    public String adminComplaints(Model model) {
        model.addAttribute(
                "complaints",
                complaintRepository.findAll(
                        org.springframework.data.domain.Sort
                                .by(org.springframework.data.domain.Sort.Direction.DESC,
                                        "createdAt")
                )
        );
        return "admin-complaints";
    }

    @GetMapping("/admin/change-password")
    public String changePasswordPage() {
        return "admin-change-password";
    }

    @PostMapping("/admin/change-password")
    public String changePassword(
            Authentication authentication,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        User admin = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            redirectAttributes.addFlashAttribute(
                    "error", "Current password is incorrect."
            );
            return "redirect:/admin/change-password";
        }

        if (newPassword.length() < 12) {
            redirectAttributes.addFlashAttribute(
                    "error", "New password must be at least 12 characters."
            );
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute(
                    "error", "New password and confirm password do not match."
            );
            return "redirect:/admin/change-password";
        }

        if (passwordEncoder.matches(newPassword, admin.getPassword())) {
            redirectAttributes.addFlashAttribute(
                    "error", "New password must be different from current password."
            );
            return "redirect:/admin/change-password";
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);

        redirectAttributes.addFlashAttribute(
                "success", "Password changed successfully."
        );

        return "redirect:/admin/change-password";
    }

    @PostMapping("/admin/users/account-status")
    public String updateAccountStatus(
            @RequestParam("id") Long id,
            @RequestParam("accountStatus") String accountStatus,
            RedirectAttributes redirectAttributes) {

        if (!"ACTIVE".equals(accountStatus)
                && !"SUSPENDED".equals(accountStatus)) {
            redirectAttributes.addFlashAttribute(
                    "error", "Invalid account status.");
            return "redirect:/admin/complaints";
        }

        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "User not found.");
            return "redirect:/admin/complaints";
        }

        if ("ADMIN".equals(user.getRole())) {
            redirectAttributes.addFlashAttribute(
                    "error", "Admin accounts cannot be suspended here.");
            return "redirect:/admin/complaints";
        }

        user.setAccountStatus(accountStatus);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "success",
                "SUSPENDED".equals(accountStatus)
                        ? "User account suspended successfully."
                        : "User account reactivated successfully."
        );

        return "redirect:/admin/complaints";
    }

    @PostMapping("/admin/approved/account-status")
    public String updateApprovedOrganizationAccountStatus(
            @RequestParam("id") Long id,
            @RequestParam("accountStatus") String accountStatus,
            RedirectAttributes redirectAttributes) {

        if (!"ACTIVE".equals(accountStatus)
                && !"SUSPENDED".equals(accountStatus)) {
            redirectAttributes.addFlashAttribute(
                    "error", "Invalid account status.");
            return "redirect:/admin/approved";
        }

        User organization = userRepository.findById(id).orElse(null);

        if (organization == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Organization not found.");
            return "redirect:/admin/approved";
        }

        if (!"APPROVED".equals(organization.getStatus())) {
            redirectAttributes.addFlashAttribute(
                    "error", "Only approved organizations can be managed here.");
            return "redirect:/admin/approved";
        }

        if ("ADMIN".equals(organization.getRole())) {
            redirectAttributes.addFlashAttribute(
                    "error", "Admin accounts cannot be suspended here.");
            return "redirect:/admin/approved";
        }

        organization.setAccountStatus(accountStatus);
        userRepository.save(organization);

        redirectAttributes.addFlashAttribute(
                "success",
                "SUSPENDED".equals(accountStatus)
                        ? "Organization account suspended successfully."
                        : "Organization account reactivated successfully."
        );

        return "redirect:/admin/approved";
    }


}
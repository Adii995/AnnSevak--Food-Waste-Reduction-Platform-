
package com.AnnSavak.AnnSavak.controller;

import com.AnnSavak.AnnSavak.entity.Complaint;
import com.AnnSavak.AnnSavak.entity.User;
import com.AnnSavak.AnnSavak.repository.ComplaintRepository;
import com.AnnSavak.AnnSavak.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;


    public ComplaintController(
            ComplaintRepository complaintRepository,
            UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/complaints/new")
    public String showComplaintForm(Model model) {
        model.addAttribute("complaint", new Complaint());
        model.addAttribute("users", userRepository.findAll());
        return "complaint-form";
    }

    @PostMapping("/complaints")
    public String submitComplaint(
            @RequestParam("reportedUserId") Long reportedUserId,
            @RequestParam("complaintReason") String complaintReason,
            @RequestParam("description") String description,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        if (complaintReason == null || complaintReason.isBlank()
                || complaintReason.length() > 100
                || description == null || description.isBlank()
                || description.length() > 5000) {
            redirectAttributes.addFlashAttribute(
                    "error", "Please enter a valid reason and description.");
            return "redirect:/complaints/new";
        }

        User reporter = userRepository.findByEmail(authentication.getName())
                .orElse(null);
        User reportedUser = userRepository.findById(reportedUserId)
                .orElse(null);

        if (reporter == null || reportedUser == null
                || reporter.getId().equals(reportedUser.getId())) {
            redirectAttributes.addFlashAttribute(
                    "error", "Please select a valid user to report.");
            return "redirect:/complaints/new";
        }

        Complaint complaint = new Complaint();
        complaint.setReportedUser(reportedUser);
        complaint.setReporter(reporter);
        complaint.setComplaintReason(complaintReason.trim());
        complaint.setDescription(description.trim());

        complaintRepository.save(complaint);

        redirectAttributes.addFlashAttribute(
                "success", "Complaint submitted successfully.");
        return "redirect:/complaints/new";
    }
}

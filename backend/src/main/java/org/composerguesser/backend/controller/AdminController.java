package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.ApproveExcerptDto;
import org.composerguesser.backend.dto.ExcerptReviewDto;
import org.composerguesser.backend.dto.UpdateStatusDto;
import org.composerguesser.backend.model.ExcerptStatus;
import org.composerguesser.backend.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for admin excerpt management.
 * All endpoints require the ADMIN role, enforced by {@link org.composerguesser.backend.security.SecurityConfig}.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Returns a paginated list of excerpts filtered by status, oldest-first.
     *
     * @param status filter by excerpt status (default: DRAFT)
     * @param page   zero-based page index (default 0)
     * @param size   items per page (default 10)
     * @return paginated excerpts with Spring Page metadata
     */
    @GetMapping("/excerpts")
    public ResponseEntity<Page<ExcerptReviewDto>> getExcerpts(
            @RequestParam(defaultValue = "DRAFT") List<ExcerptStatus> status,
            @RequestParam(required = false) Long composerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getExcerpts(status, composerId, page, size));
    }

    /**
     * Flips an excerpt's status without touching its metadata.
     * Used for reject, unreject, delete, and restore operations.
     *
     * @param id  the excerpt to update
     * @param dto the target status
     * @return 200 on success, 400 if the excerpt is not found
     */
    @PatchMapping("/excerpts/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusDto dto) {
        try {
            adminService.updateStatus(id, dto.getStatus());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Updates a excerpt's metadata and sets its status to ACTIVE,
     * making it eligible for the daily challenge.
     *
     * @param id  the excerpt to approve
     * @param dto updated metadata (composer, work, title, year, description)
     * @return 200 on success, 400 if validation fails
     */
    @PatchMapping("/excerpts/{id}/approve")
    public ResponseEntity<?> approveExcerpt(@PathVariable Long id, @RequestBody ApproveExcerptDto dto) {
        try {
            adminService.approveExcerpt(id, dto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

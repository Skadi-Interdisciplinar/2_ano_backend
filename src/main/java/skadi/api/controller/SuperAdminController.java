package skadi.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.SupportRequestResponse;
import skadi.api.dto.SupportRequestUpdateRequest;
import skadi.api.dto.SupportStatusUpdateRequest;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.model.User;
import skadi.api.service.SuperAdminService;
import skadi.api.service.SupportRequestService;

import java.util.List;

@RestController
@RequestMapping("super_admin")
public class SuperAdminController {

    private final SuperAdminService service;
    private final SupportRequestService supportRequestService;

    public SuperAdminController(SuperAdminService service, SupportRequestService supportRequestService) {
        this.service = service;
        this.supportRequestService = supportRequestService;
    }

    @GetMapping("employees_admin")
    public ResponseEntity<List<UserResponse>> getAllAdmins(){
        return ResponseEntity.ok(service.getAllAdmins());
    }

    @PutMapping("employees_admin/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @RequestBody UserRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("employees_admin/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("support")
    public ResponseEntity<List<SupportRequestResponse>> findAllSupportRequests() {
        return ResponseEntity.ok(supportRequestService.findAll());
    }

    @GetMapping("support/{id}")
    public ResponseEntity<SupportRequestResponse> findSupportRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(supportRequestService.findById(id));
    }

    @PutMapping("support/{id}")
    public ResponseEntity<SupportRequestResponse> updateSupportRequest(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(supportRequestService.update(id, request, user));
    }

    @PatchMapping("support/{id}/status")
    public ResponseEntity<SupportRequestResponse> updateSupportRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody SupportStatusUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(supportRequestService.updateStatus(id, request.status(), user));
    }

    @DeleteMapping("support/{id}")
    public ResponseEntity<Void> deleteSupportRequest(@PathVariable Long id) {
        supportRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

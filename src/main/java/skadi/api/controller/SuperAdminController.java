package skadi.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.service.SuperAdminService;

import java.util.List;

@RestController
@RequestMapping("super_admin")
public class SuperAdminController {

    private final SuperAdminService service;

    public SuperAdminController(SuperAdminService service) {
        this.service = service;
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
}

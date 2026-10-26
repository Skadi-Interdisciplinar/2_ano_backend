package skadi.api.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.AdminListResponse;
import skadi.api.dto.RegisterRequest;
import skadi.api.dto.UserResponse;
import skadi.api.service.UserService;

@RestController
@RequestMapping("/super_admin")
@AllArgsConstructor
public class SuperAdminController {

    private final UserService service;

    @GetMapping("/get_admins")
    public ResponseEntity<AdminListResponse> getAdmins(){
        return ResponseEntity.ok(service.getAdminsSvc());
    }

    @PostMapping("/admins")
    public ResponseEntity<UserResponse> createAdmin(@RequestBody RegisterRequest dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAdmin(dto));
    }

    @PutMapping("/admins/{id}")
    public ResponseEntity<UserResponse> updateAdmin(@PathVariable Long id, @RequestBody RegisterRequest dto){
        return ResponseEntity.ok(service.updateAdmin(id, dto));
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id){
        service.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }
}

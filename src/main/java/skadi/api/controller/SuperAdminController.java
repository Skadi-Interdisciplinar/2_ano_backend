package skadi.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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


}

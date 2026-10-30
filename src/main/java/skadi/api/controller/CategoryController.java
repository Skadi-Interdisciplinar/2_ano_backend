package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.CategoryRequest;
import skadi.api.dto.CategoryResponse;
import skadi.api.service.CategoryService;
import skadi.api.model.User;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Categorias")
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal User user
    ) {
        CategoryResponse response = service.create(request, user);
        return ResponseEntity.created(URI.create("/categories/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}

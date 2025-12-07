package com.vexa.vantage.controller;

import com.vexa.vantage.exception.DependencyException;
import com.vexa.vantage.model.User;
import com.vexa.vantage.security.UserDetailsImpl;
import com.vexa.vantage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/company")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersByCompany(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Assuming user details has company ID, or we fetch it via user service using
        // ID
        // For simplicity, let's look up the user object to get company if not in
        // details
        User currentUser = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        Long companyId = currentUser.getCompany().getId();

        return ResponseEntity.ok(userService.getAllUsersByCompany(companyId));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createUser(@RequestBody User user, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User creator = userService.findByEmail(userDetails.getUsername()).orElseThrow();
            User createdUser = userService.createUser(user, creator);
            return ResponseEntity.ok(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestParam(required = false) Long replacementUserId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            User executor = userService.findByEmail(userDetails.getUsername()).orElseThrow();
            userService.deactivateUser(id, replacementUserId, executor);
            return ResponseEntity.ok(Map.of("message", "Usuario desactivado correctamente."));
        } catch (DependencyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "type", e.getType(),
                    "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        try {
            userService.activateUser(id);
            return ResponseEntity.ok(Map.of("message", "Usuario reactivado correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

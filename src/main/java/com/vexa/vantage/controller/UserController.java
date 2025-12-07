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
        User currentUser = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        Long companyId = currentUser.getCompany().getId();

        return ResponseEntity.ok(userService.getUsersForRole(companyId, currentUser.getRole()));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PO') or hasRole('SM') or hasRole('OWNER')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User currentUser = userService.findByEmail(userDetails.getUsername()).orElseThrow();
            Long companyId = currentUser.getCompany().getId();
            com.vexa.vantage.model.RoleType roleType = com.vexa.vantage.model.RoleType.valueOf(role.toUpperCase());
            return ResponseEntity.ok(userService.getUsersByRole(companyId, roleType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody User user, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User creator = userService.findByEmail(userDetails.getUsername()).orElseThrow();

            // Validación extra para ADMIN
            if (creator.getRole().name().equals("ROLE_ADMIN")) {
                if (user.getRole().name().equals("ROLE_OWNER") || user.getRole().name().equals("ROLE_ADMIN")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Como Administrador, solo puedes crear PO, Scrum Masters y Desarrolladores."));
                }
            }

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
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User executor = userService.findByEmail(userDetails.getUsername()).orElseThrow();
            User targetUser = userService.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validación extra para ADMIN
            if (executor.getRole().name().equals("ROLE_ADMIN")) {
                // No puede editar al Owner ni a otros Admins
                if (targetUser.getRole().name().equals("ROLE_OWNER")
                        || targetUser.getRole().name().equals("ROLE_ADMIN")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "No tienes permisos para editar a este usuario."));
                }
                // No puede promover a nadie a Owner o Admin
                if (user.getRole().name().equals("ROLE_OWNER") || user.getRole().name().equals("ROLE_ADMIN")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "No puedes asignar roles de Administrador o Owner."));
                }
            }

            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

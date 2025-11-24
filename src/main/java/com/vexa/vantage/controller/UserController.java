package com.vexa.vantage.controller;

import com.vexa.vantage.model.User;
import com.vexa.vantage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.vexa.vantage.model.RoleType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // DTO para actualizar perfil
    public static class UpdateProfileRequest {
        private String fullName;
        private String email;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // DTO para cambiar contraseña
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    if (request.getFullName() != null && !request.getFullName().isEmpty()) {
                        user.setFullName(request.getFullName());
                    }
                    if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                        // Verificar que el email no esté en uso por otro usuario
                        if (userRepository.existsByEmail(request.getEmail()) &&
                                !user.getEmail().equals(request.getEmail())) {
                            return ResponseEntity.badRequest()
                                    .body(Map.of("message", "El email ya está en uso"));
                        }
                        user.setEmail(request.getEmail());
                    }
                    User updatedUser = userRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", "Perfil actualizado correctamente",
                            "user", Map.of(
                                    "id", updatedUser.getId(),
                                    "fullName", updatedUser.getFullName(),
                                    "email", updatedUser.getEmail())));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody ChangePasswordRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    // Verificar contraseña actual
                    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "La contraseña actual es incorrecta"));
                    }

                    // Actualizar contraseña
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(user);

                    return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUsersByRole(@PathVariable String roleName) {
        try {
            // Asumimos que el frontend envía "ROLE_SM", "ROLE_PO", etc.
            RoleType roleType = RoleType.valueOf(roleName);
            List<User> users = userRepository.findByRoles_Name(roleType);

            // Mapear a una respuesta simple para no devolver password ni datos sensibles
            List<Map<String, Object>> userDtos = users.stream()
                    .map(u -> Map.<String, Object>of(
                            "id", u.getId(),
                            "email", u.getEmail(),
                            "fullName", u.getFullName()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol no válido"));
        }
    }
}

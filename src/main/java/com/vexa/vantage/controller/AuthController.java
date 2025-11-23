package com.vexa.vantage.controller;

import com.vexa.vantage.model.Role;
import com.vexa.vantage.model.RoleType;
import com.vexa.vantage.model.User;
import com.vexa.vantage.repository.RoleRepository;
import com.vexa.vantage.repository.UserRepository;
import com.vexa.vantage.security.JwtUtils;
import com.vexa.vantage.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                roles));
    }

    // REGISTRO
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: El email ya está en uso!"));
        }

        // Crear nuevo usuario
        User user = new User(signUpRequest.getFullName(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(RoleType.ROLE_DEV)
                    .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(adminRole);
                        break;
                    case "po":
                        Role poRole = roleRepository.findByName(RoleType.ROLE_PO)
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(poRole);
                        break;
                    case "sm":
                        Role smRole = roleRepository.findByName(RoleType.ROLE_SM)
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(smRole);
                        break;
                    default:
                        Role devRole = roleRepository.findByName(RoleType.ROLE_DEV)
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(devRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Usuario registrado exitosamente!"));
    }
}

// CLASES AUXILIARES (DTOs) PARA RECIBIR DATOS
// Puedes ponerlas en archivos separados en un paquete 'payload' o aquí mismo
// abajo por simplicidad.
// Por orden, te recomiendo ponerlas aquí abajo temporalmente para que compile
// rápido.

@lombok.Data
class LoginRequest {
    private String email;
    private String password;
}

@lombok.Data
class SignupRequest {
    private String fullName;
    private String email;
    private Set<String> role;
    private String password;
}

@lombok.Data
@lombok.AllArgsConstructor
class JwtResponse {
    private String token;
    private Long id;
    private String email;
    private List<String> roles;
}

@lombok.Data
@lombok.AllArgsConstructor
class MessageResponse {
    private String message;
}
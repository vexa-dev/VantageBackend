package com.vexa.vantage.controller;

import com.vexa.vantage.model.RoleType;
import com.vexa.vantage.model.User;
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
    com.vexa.vantage.repository.CompanyRepository companyRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        try {
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
                    userDetails.getFullName(),
                    roles));
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: Tu cuenta ha sido deshabilitada. Contacta al administrador."));
        } catch (org.springframework.security.core.AuthenticationException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: Credenciales inválidas."));
        }
    }

    // REGISTRO
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: El email ya está en uso!"));
        }

        // Validar nombre de empresa
        if (signUpRequest.getCompanyName() == null || signUpRequest.getCompanyName().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: El nombre de la empresa es obligatorio."));
        }

        // Crear Empresa
        com.vexa.vantage.model.Company company = new com.vexa.vantage.model.Company(
                signUpRequest.getCompanyName(),
                signUpRequest.getSubscriptionType() != null ? signUpRequest.getSubscriptionType() : "FREE");
        company = companyRepository.save(company);

        // Crear usuario OWNER
        User user = new User(signUpRequest.getFullName(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        user.setRole(RoleType.ROLE_OWNER); // Siempre es OWNER en este flujo
        user.setCompany(company); // Asignar a la empresa creada

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Empresa y Dueño registrados exitosamente!"));
    }
}

// CLASES AUXILIARES (DTOs) PARA RECIBIR DATOS
@lombok.Data
class LoginRequest {
    private String email;
    private String password;
}

@lombok.Data
class SignupRequest {
    private String fullName;
    private String email;
    private String companyName; // Nuevo campo
    private String subscriptionType; // Nuevo campo (opcional)
    private Set<String> role; // Ignorado en este flujo, pero mantenido por compatibilidad si es necesario o
                              // se puede quitar
    private String password;
}

@lombok.Data
@lombok.AllArgsConstructor
class JwtResponse {
    private String token;
    private Long id;
    private String email;
    private String fullName;
    private List<String> roles;
}

@lombok.Data
@lombok.AllArgsConstructor
class MessageResponse {
    private String message;
}
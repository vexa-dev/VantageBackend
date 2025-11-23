package com.vexa.vantage.service;

import com.vexa.vantage.model.User;
import com.vexa.vantage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Buscar usuario por email (Útil para el Login)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Verificar si existe
    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Guardar usuario (Lo usaremos en el registro)
    public User save(User user) {
        return userRepository.save(user);
    }
}
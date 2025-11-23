package com.vexa.vantage;

import com.vexa.vantage.model.Role;
import com.vexa.vantage.model.RoleType;
import com.vexa.vantage.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Verificar si la tabla de roles está vacía
        if (roleRepository.count() == 0) {
            System.out.println("⚡ Inicializando Roles en la Base de Datos...");

            roleRepository.save(new Role(RoleType.ROLE_ADMIN));
            roleRepository.save(new Role(RoleType.ROLE_PO));
            roleRepository.save(new Role(RoleType.ROLE_SM));
            roleRepository.save(new Role(RoleType.ROLE_DEV));

            System.out.println("✅ Roles creados exitosamente: ADMIN, PO, SM, DEV");
        }
    }
}
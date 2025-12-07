package com.vexa.vantage.service;

import com.vexa.vantage.exception.DependencyException;
import com.vexa.vantage.model.Issue;
import com.vexa.vantage.model.IssueStatus;
import com.vexa.vantage.model.RoleType;
import com.vexa.vantage.model.User;
import com.vexa.vantage.model.Project;
import com.vexa.vantage.repository.IssueRepository;
import com.vexa.vantage.repository.ProjectRepository;
import com.vexa.vantage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    // Buscar usuario por email (Útil para el Login)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Verificar si existe
    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Guardar usuario (Lo usaremos en el registro)
    public User save(User user) {
        return userRepository.save(user);
    }

    // Listar usuarios filtrados según el rol del solicitante
    public List<User> getUsersForRole(Long companyId, RoleType requesterRole) {
        List<User> allUsers = userRepository.findAllByCompanyId(companyId);

        if (requesterRole == RoleType.ROLE_ADMIN) {
            // Admin solo ve PO, SM y DEV
            return allUsers.stream()
                    .filter(u -> u.getRole() == RoleType.ROLE_PO ||
                            u.getRole() == RoleType.ROLE_SM ||
                            u.getRole() == RoleType.ROLE_DEV)
                    .toList();
        }

        // Owner ve todo
        return allUsers;
    }

    public List<User> getUsersByRole(Long companyId, RoleType role) {
        return userRepository.findAllByCompanyId(companyId).stream()
                .filter(u -> u.getRole() == role)
                .toList();
    }

    // Crear usuario genérico (Validado por controller, pero aquí validamos lógica
    // extra)
    // Crear usuario genérico (Validado por controller, pero aquí validamos lógica
    // extra)
    public User createUser(User user, User creator) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El correo electrónico ya está registrado.");
        }

        if (!creator.getRole().equals(RoleType.ROLE_OWNER)) {
            // Solo el owner puede crear Admins, pero en general el endpoint valida mejor
            if (user.getRole().equals(RoleType.ROLE_ADMIN)) {
                throw new RuntimeException("Solo el Owner puede crear Administradores.");
            }
        }

        user.setCompany(creator.getCompany());
        user.setPassword(encoder.encode(user.getPassword()));
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Long targetUserId, Long replacementUserId, User executor) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Hierarchy Validation
        if (targetUser.getId().equals(executor.getId())) {
            throw new RuntimeException("No puedes eliminar tu propia cuenta.");
        }

        if (executor.getRole() == RoleType.ROLE_ADMIN && targetUser.getRole() == RoleType.ROLE_OWNER) {
            throw new RuntimeException("Un Admin no puede eliminar al Owner.");
        }

        // 2. Role Validation
        if (targetUser.getRole() == RoleType.ROLE_PO || targetUser.getRole() == RoleType.ROLE_SM) {
            // Check ProjectRepository for active projects
            List<Project> ownerProjects = projectRepository.findByOwner(targetUser);
            List<Project> smProjects = projectRepository.findByScrumMaster(targetUser);

            // Filter only active projects if needed, assuming all returned are relevant
            if (!ownerProjects.isEmpty() || !smProjects.isEmpty()) {
                throw new DependencyException("CRITICAL_DEPENDENCY",
                        "Este usuario gestiona proyectos activos (Owner/SM). Debe reasignarlos primero en la configuración del proyecto.");
            }
        } else if (targetUser.getRole() == RoleType.ROLE_DEV) {
            // Check IssueRepository for active issues (NOT DONE)
            List<Issue> activeIssues = issueRepository.findByAssigneesContainingAndStatusNot(targetUser,
                    IssueStatus.DONE);

            if (!activeIssues.isEmpty()) {
                if (replacementUserId == null) {
                    throw new DependencyException("REASSIGNMENT_NEEDED",
                            "El usuario tiene " + activeIssues.size() + " tareas pendientes. Seleccione un reemplazo.");
                } else {
                    // Reassign issues
                    User replacement = userRepository.findById(replacementUserId)
                            .orElseThrow(() -> new RuntimeException("Usuario de reemplazo no encontrado"));

                    for (Issue issue : activeIssues) {
                        issue.getAssignees().remove(targetUser);
                        issue.getAssignees().add(replacement);
                        issueRepository.save(issue);
                    }
                }
            }
        }

        targetUser.setActive(false);
        userRepository.save(targetUser);
    }

    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setActive(true);
        userRepository.save(user);
    }

    public User updateUser(Long id, User updatedInfo) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setFullName(updatedInfo.getFullName());
        user.setRole(updatedInfo.getRole());

        // Optional: Allow email update if valid
        if (updatedInfo.getEmail() != null && !updatedInfo.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updatedInfo.getEmail())) {
                throw new RuntimeException("El email ya está en uso");
            }
            user.setEmail(updatedInfo.getEmail());
        }

        return userRepository.save(user);
    }
}
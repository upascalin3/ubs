package com.utility.billing.auth.config;

import com.utility.billing.auth.entity.Role;
import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.entity.UserStatus;
import com.utility.billing.auth.repository.RoleRepository;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.common.security.RoleName;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUser("admin@wasac.rw", "0780000000", "System Administrator", RoleName.ADMIN, "Admin@123");
        seedUser("operator@wasac.rw", "0780000001", "Meter Operator", RoleName.OPERATOR, "Password@123");
        seedUser("finance@wasac.rw", "0780000002", "Finance Officer", RoleName.FINANCE, "Password@123");
    }

    private void seedUser(String email, String phone, String fullName, String roleName, String password) {
        Role role = roleRepository.findByRoleName(roleName).orElseThrow();
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder()
                        .email(email)
                        .phoneNumber(phone)
                        .build());

        user.setFullName(fullName);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setMustChangePassword(false);
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }
}

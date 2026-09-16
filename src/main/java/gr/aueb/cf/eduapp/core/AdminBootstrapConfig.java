package gr.aueb.cf.eduapp.core;

import gr.aueb.cf.eduapp.model.Role;
import gr.aueb.cf.eduapp.model.User;
import gr.aueb.cf.eduapp.repository.RoleRepository;
import gr.aueb.cf.eduapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;

@Configuration                                       // "This class contains beans that you must create during startup."
@RequiredArgsConstructor                    // Creates automatically a constructor-with all final fields included-so that Spring performs Dependency Injection.
@Slf4j                                  // creates a log object automatically in the background so that we can write: log.warn(...) without having to create a separate logger.
public class AdminBootstrapConfig {

    private static final String ADMIN_ROLE_NAME = "ADMIN";
    private static final String ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdmin() {   // CLR is a special springBoot interface. Whichever bean implements it, it automatically runs ONCE, right after the boot of the app!
        return args -> {
            if (userRepository.existsByRole_Name(ADMIN_ROLE_NAME)) {
                log.debug("Admin user already exists, skipping bootstrap.");
                return;
            }

            Role adminRole = roleRepository.findAllByOrderByNameAsc().stream()        // searches which role entity corresponds to the name: "ADMIN".
                    .filter(r -> r.getName().equals(ADMIN_ROLE_NAME))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Role 'ADMIN' not found. Check V3__insert_roles_capabilites.sql"));

            String tempPassword = generateSecurePassword();  // Method that creates a random secure password. See at the bottom of this class.

            User admin = new User();
            admin.setUsername(ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(tempPassword));              // BCrypt
            adminRole.addUser(admin);

            userRepository.save(admin);

            log.warn("=========================================================");
            log.warn(" FIRST ADMIN USER CREATED");
            log.warn(" username: {}", ADMIN_USERNAME);                         // stated as WARN in order to be easily visible within the log.
            log.warn(" password: {}", tempPassword);                       // password is presented in the logs ONLY (NOT IN ANY HTTP RESPONSE. NOT VIA INTERNET!)
            log.warn(" (Please change your password after the first login!)");
            log.warn("=========================================================");
        };
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();        // secureRandom -> safer than plain random
        byte[] bytes = new byte[18];                     // 18 bytes of random data converted into a Base64 string (letters/numbers) safe to de included in a URL/JSON. Resulting in a 24 character-password.
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.dto.PasswordUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.dto.UserReadOnlyDTO;
import gr.aueb.cf.eduapp.mapper.Mapper;
import gr.aueb.cf.eduapp.model.Role;
import gr.aueb.cf.eduapp.model.User;
import gr.aueb.cf.eduapp.repository.RoleRepository;
import gr.aueb.cf.eduapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final Mapper mapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

//    @Autowired
//    public UserService(UserRepository userRepository, Mapper mapper, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.mapper = mapper;
//        this.roleRepository = roleRepository;
//        this.passwordEncoder = passwordEncoder;
//    }

    @Override
    @Transactional(rollbackFor = {EntityAlreadyExistsException.class, EntityInvalidArgumentException.class})
    public UserReadOnlyDTO saveUser(UserInsertDTO userInsertDTO)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException {
        try {
            if (userRepository.findByUsername(userInsertDTO.username()).isPresent()) {
                throw new EntityAlreadyExistsException("User", "User with username=" + userInsertDTO.username() + " already exists");
            }

            User user = mapper.mapToUserEntity(userInsertDTO);
            user.setPassword(passwordEncoder.encode(userInsertDTO.password()));

            Role role = roleRepository.findById(userInsertDTO.roleId())
                    .orElseThrow(() -> new EntityInvalidArgumentException("Role", "Role with id=" + userInsertDTO.roleId() + " does not exist"));
            role.addUser(user);
            userRepository.save(user);
            log.info("User with username={} saved successfully", userInsertDTO.username());
            return mapper.mapToUserReadOnlyDTO(user);
        } catch (EntityAlreadyExistsException e) {
            log.error("Save failed. User with username={} already exists", userInsertDTO.username());
            throw e;
        } catch (EntityInvalidArgumentException e) {
            log.error("Save failed. Invalid arguments for user with username={}", userInsertDTO.username());
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_USER')")
    @Transactional(readOnly = true)
    public UserReadOnlyDTO getUserByUUID(UUID uuid) throws EntityNotFoundException {
        try {
            User user = userRepository.findByUuid(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid=" + uuid + " not found"));

            log.debug("User with uuid={} found successfully", uuid);
            return mapper.mapToUserReadOnlyDTO(user);
        } catch (EntityNotFoundException e) {
            log.error("Get failed. User with uuid={} not found", uuid);
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_USER')")
    @Transactional(readOnly = true)
    public UserReadOnlyDTO getUserByUUIDDeletedFalse(UUID uuid) throws EntityNotFoundException {
        try {
            User user = userRepository.findByUuidAndDeletedFalse(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid=" + uuid + " not found"));

            log.debug("Active user with uuid={} found successfully", uuid);
            return mapper.mapToUserReadOnlyDTO(user);
        } catch (EntityNotFoundException e) {
            log.error("Get failed. Active user with uuid={} not found", uuid);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    @PreAuthorize("@securityService.isOwnUserProfile(#uuid, authentication)")
    @Transactional(rollbackFor = EntityInvalidArgumentException.class)
    public void changePassword(UUID uuid, PasswordUpdateDTO dto)
            throws EntityNotFoundException, EntityInvalidArgumentException {
        try {
            User user = userRepository.findByUuid(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid=" + uuid + " not found"));

            if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {                 // Always "matches" -> NEVER "equals" as the password is BCrypted (one-way hashed) and there is no way to be decoded!!! SOS!
                throw new EntityInvalidArgumentException("Password", "Old password is incorrect");   // the command -> passwordEncoder.matches(plainText, hashedText) gets the dto.oldpassword() re-hashes it using the same algorithm and compares it with the already hashed user.getPassword(). If they match, the initial password(old) is right!
            }

            user.setPassword(passwordEncoder.encode(dto.newPassword()));
            userRepository.save(user);
            log.info("Password changed successfully for user with uuid={}", uuid);
        } catch (EntityInvalidArgumentException e) {
            log.error("Change password failed for user with uuid={}. Old password incorrect", uuid);
            throw e;
        }
    }
}

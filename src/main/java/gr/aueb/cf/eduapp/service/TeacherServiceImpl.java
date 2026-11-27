package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.enums.AllowedFileType;
import gr.aueb.cf.eduapp.core.enums.RoleName;
import gr.aueb.cf.eduapp.core.exceptions.*;
import gr.aueb.cf.eduapp.core.filters.TeacherFilters;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.mapper.Mapper;
import gr.aueb.cf.eduapp.model.*;
import gr.aueb.cf.eduapp.repository.*;
import gr.aueb.cf.eduapp.specification.TeacherSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class TeacherServiceImpl implements ITeacherService {

    private final TeacherRepository teacherRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PersonalInfoRepository personalInfoRepository;
    private final Mapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    @Transactional(rollbackFor = { EntityAlreadyExistsException.class, EntityInvalidArgumentException.class, ServerConfigurationException.class } )
    public TeacherReadOnlyDTO saveTeacher(TeacherInsertDTO dto)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException, ServerConfigurationException {

        try {
            if (dto.vat() != null && teacherRepository.findByVat(dto.vat()).isPresent()) {
                throw new EntityAlreadyExistsException("Teacher", "Teacher with this vat already exists");
            }
            if (personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka()).isPresent()) {
                throw new EntityAlreadyExistsException("AMKA", "User with this AMKA already exists");
            }

            if (userRepository.findByUsername(dto.userInsertDTO().username()).isPresent()) {
                throw new EntityAlreadyExistsException("Username", "User with this username already exists");
            }

            if (personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()).isPresent()) {
                throw new EntityAlreadyExistsException("IdentityNumber", "User with this identity number already exists");
            }

            Region region = regionRepository.findById(dto.regionId())
                    .orElseThrow(() -> new EntityInvalidArgumentException("Region", "Region id=" + dto.regionId() + " invalid"));

            Role role = roleRepository.findByName(RoleName.TEACHER.name())      // name() -> returns the actual roleName "TEACHER"
                    .orElseThrow(() -> new ServerConfigurationException("Role","Role '%s' not found in database. Check that the Flyway migrations in db/migration have been applied.".formatted(RoleName.TEACHER.name())));

            Teacher teacher = mapper.mapToTeacherEntity(dto);

            User user = teacher.getUser();
            user.setPassword(passwordEncoder.encode(dto.userInsertDTO().password()));

            region.addTeacher(teacher);

            role.addUser(user);

            teacherRepository.save(teacher);        // saved teacher

            log.info("Teacher with uuid={} saved successfully.", teacher.getUuid());
            return mapper.mapToTeacherReadOnlyDTO(teacher);
        } catch (EntityAlreadyExistsException e) {
            log.warn("Save failed. Teacher already exists", e);
            throw e;
        } catch (EntityInvalidArgumentException e) {
            log.warn("Save failed. Invalid argument", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeacherExists(String vat) {
        return teacherRepository.findByVat(vat).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAmkaExists(String amka) {
        return personalInfoRepository.findByAmka(amka).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isIdentityNumberExists(String identityNumber) {
        return personalInfoRepository.findByIdentityNumber(identityNumber).isPresent();
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_TEACHERS')")
    @Transactional(readOnly = true)
    public Page<TeacherReadOnlyDTO> getPaginatedTeachers(Pageable pageable) {
        Page<Teacher> teachersPage = teacherRepository.findAll(pageable);
        log.debug("Get paginated returned successfully page={} and size={}", teachersPage.getNumber(), teachersPage.getSize());
        return teachersPage.map(mapper::mapToTeacherReadOnlyDTO);
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_TEACHERS')")
    @Transactional(readOnly = true)
    public Page<TeacherReadOnlyDTO> getPaginatedTeachersDeletedFalse(Pageable pageable) {
        Page<Teacher> teachersPage = teacherRepository.findAllByDeletedFalse(pageable);
        log.debug("Get paginated not deleted returned successfully page={} and size={}", teachersPage.getNumber(), teachersPage.getSize());
        return teachersPage.map(mapper::mapToTeacherReadOnlyDTO);
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_TEACHER')")
    @Transactional(rollbackFor = { EntityNotFoundException.class, EntityAlreadyExistsException.class, EntityInvalidArgumentException.class} )
    public TeacherReadOnlyDTO updateTeacher(TeacherUpdateDTO dto)
            throws EntityNotFoundException, EntityAlreadyExistsException, EntityInvalidArgumentException {
        try {
            Teacher teacher = teacherRepository.findByUuid(dto.uuid())
                    .orElseThrow(() -> new EntityNotFoundException("Teacher", "Teacher with uuid=" + dto.uuid() + " not found"));

            teacher.setFirstname(dto.firstname());
            teacher.setLastname(dto.lastname());

            if (!teacher.getVat().equals(dto.vat())) {
                if (teacherRepository.findByVat(dto.vat()).isPresent()) {
                    throw new EntityAlreadyExistsException("Teacher","Teacher with this vat already exists");
                }
                teacher.setVat(dto.vat());
            }

            if (!teacher.getPersonalInfo().getIdentityNumber().equals(dto.personalInfoUpdateDTO().identityNumber()) &&
                    personalInfoRepository.findByIdentityNumber(dto.personalInfoUpdateDTO().identityNumber()).isPresent()) {
                throw new EntityAlreadyExistsException("Teacher", "Teacher with this identity number already exists");
            }

            if (!Objects.equals(dto.regionId(), teacher.getRegion().getId())) {
                Region region = regionRepository.findById(dto.regionId())
                        .orElseThrow(() -> new EntityInvalidArgumentException("Region","Region id=" + dto.regionId() + " invalid"));
                Region oldRegion = teacher.getRegion();
                if (oldRegion != null) {
                    oldRegion.removeTeacher(teacher);
                }
                region.addTeacher(teacher);
            }
            // user username and password updated TODO
            // other features to be updated TODO

            teacherRepository.save(teacher);    // προαιρετικό αν είναι managed
            log.info("Teacher with uuid={} updated successfully", dto.uuid());
            return mapper.mapToTeacherReadOnlyDTO(teacher);
        } catch (EntityNotFoundException e) {
            log.error("Update failed for teacher with uuid={}. Teacher not found", dto.uuid(), e);
            throw e;
        } catch (EntityAlreadyExistsException e) {
            log.error("Update failed for teacher with uuid={}. Teacher already exists", dto.uuid(), e);
            throw e;
        } catch (EntityInvalidArgumentException e) {
            log.error("Update failed for teacher with uuid={}. Region id={} invalid", dto.uuid(), dto.regionId(), e);
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('DELETE_TEACHER')")
    @Transactional(rollbackFor = EntityNotFoundException.class)
    public TeacherReadOnlyDTO deleteTeacherByUUID(UUID uuid) throws EntityNotFoundException {
        try {
            Teacher teacher = teacherRepository.findByUuidAndDeletedFalse(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("Teacher","Teacher with uuid=" + uuid + " not found"));

            teacher.softDelete();
            teacher.getPersonalInfo().softDelete();
            teacher.getUser().softDelete();
            // No save needed if Teacher is managed
//            teacherRepository.save(teacher);
            log.info("Teacher with uuid={} deleted successfully", uuid);
            return mapper.mapToTeacherReadOnlyDTO(teacher);
        } catch (EntityNotFoundException e) {
            log.error("Update failed for teacher with uuid={}. Teacher not found", uuid, e);

            // Automatic rollback due to @Transactional annotation
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_TEACHER') or (hasAuthority('VIEW_ONLY_TEACHER') and @securityService.isOwnTeacherProfile(#uuid, authentication))")
    @Transactional(readOnly = true)
    public TeacherReadOnlyDTO getTeacherByUUIDDeletedFalse(UUID uuid) throws EntityNotFoundException {
        try {
            Teacher teacher = teacherRepository.findByUuidAndDeletedFalse(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("Teacher","Teacher with uuid=" + uuid + " not found"));
            log.debug("Get non-deleted teacher by uuid={} returned successfully", uuid);
            return mapper.mapToTeacherReadOnlyDTO(teacher);
        } catch (EntityNotFoundException e) {
            log.error("Get teacher by uuid={} failed", uuid, e);
            throw e;
        }
    }

    @Override
    @Retryable(
            includes = { IOException.class, HttpServerErrorException.class },
            maxRetries = 2,
            delay = 2000,
            multiplier = 2,
            maxDelay = 10000
    )
    @Transactional(rollbackFor = EntityNotFoundException.class)
    @PreAuthorize("hasAuthority('UPLOAD_OWN_AMKA_FILE') and @securityService.isOwnTeacherProfile(#uuid, authentication)")
    public void saveAmkaFile(UUID uuid, MultipartFile amkaFile)
            throws FileUploadException, EntityNotFoundException, InvalidFileException {

        if (amkaFile.isEmpty()) {
            throw new InvalidFileException("TeacherAmka", "The uploaded file is empty");
        }

        try {

            Teacher teacher = teacherRepository.findByUuid(uuid).orElseThrow(()
                    -> new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid + " not found"));   // fail-test

            PersonalInfo personalInfo = teacher.getPersonalInfo();

            String originalFilename = amkaFile.getOriginalFilename();

            Tika tika = new Tika();        // detects the real file type from its content, not from the filename or Content-Type
            String contentType = tika.detect(amkaFile.getBytes());

            AllowedFileType allowedFileType = AllowedFileType.getByContentType(contentType)
                    .orElseThrow(() -> new InvalidFileException("TeacherAmka", "The file type is not supported. " +
                            "Please upload a valid amka-file in pdf, png or jpg format"));

            String savedName = UUID.randomUUID() + allowedFileType.getExtension();
            Path filePath = Path.of(uploadDir).resolve(savedName);

            Path oldFilePath = personalInfo.getAmkaFile() != null               // Keeping the old file path before replacing it (null if there is none)
                    ? Path.of(personalInfo.getAmkaFile().getFilePath())
                    : null;

            Files.createDirectories(filePath.getParent());       // Writes the new file and registers its cleanup immediately
            amkaFile.transferTo(filePath);                    // safe for large files, more efficient
            registerFileCleanup(filePath, oldFilePath);

            Attachment attachment = new Attachment();                       // Updating the database
            attachment.setFilename(originalFilename);
            attachment.setSavedName(savedName);
            attachment.setFilePath(filePath.toString());
            attachment.setContentType(contentType);
            attachment.setExtension(allowedFileType.getExtension());

            if (personalInfo.getAmkaFile() != null) {
                personalInfo.removeAmkaFile();                          // orphanRemoval handles DB deletion
            }
            personalInfo.addAmkaFile(attachment);
            teacherRepository.save(teacher);
            log.info("Attachment for teacher with uuid={} saved", uuid);
        } catch (EntityNotFoundException e) {
            log.error("Teacher with uuid={} not found", uuid, e);
            throw e;
        } catch (IOException | HttpServerErrorException e) {
            log.error("Error saving attachment for teacher with uuid={}", uuid, e);
            throw new FileUploadException("TeacherAmka", "Error saving attachment for teacher with uuid=" + uuid);
        }
    }


    @Override
    @PreAuthorize("hasAuthority('VIEW_TEACHERS')")
    @Transactional(readOnly = true)
    public Page<TeacherReadOnlyDTO> getTeachersPaginatedFiltered(Pageable pageable, TeacherFilters filters)
            throws EntityNotFoundException {
        try {
            if (filters.getUuid() != null) {
                Teacher teacher = teacherRepository.findByUuid(filters.getUuid())
                        .orElseThrow(() -> new EntityNotFoundException("Teacher", "uuid=" + filters.getUuid() + " not found"));
                return singleResultPage(teacher, pageable);
            }

            if (filters.getVat() != null) {
                Teacher teacher = teacherRepository.findByVat(filters.getVat())
                        .orElseThrow(() -> new EntityNotFoundException("Teacher", "Teacher with this vat was not found"));
                return singleResultPage(teacher, pageable);
            }

            if (filters.getAmka() != null) {
                Teacher teacher = teacherRepository.findByPersonalInfo_Amka(filters.getAmka())
                        .orElseThrow(() -> new EntityNotFoundException("Teacher", "Teacher with this amka was not found"));
                return singleResultPage(teacher, pageable);
            }

            var filtered = teacherRepository.findAll(TeacherSpecification.build(filters), pageable);

            log.debug("Filtered and paginated teachers were returned successfully with page={} and size={}", pageable.getPageNumber(),
                    pageable.getPageSize());
            return filtered.map(mapper::mapToTeacherReadOnlyDTO);
        } catch (EntityNotFoundException e) {
            log.error("Filtered and paginated teachers were not found", e);
            throw e;
        }
    }

    private Page<TeacherReadOnlyDTO> singleResultPage(Teacher teacher, Pageable pageable) {
        return new PageImpl<>(
                List.of(mapper.mapToTeacherReadOnlyDTO(teacher)),
                pageable,
                1
        );
    }

    /**
     * Runs when the transaction finishes:
     * - committed  -> the old file is no longer needed, delete it
     * - rolled back -> the new file is not referenced by the database, delete it
     */
    private void registerFileCleanup(Path newFile, Path oldFile) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                Path fileToDelete = (status == STATUS_COMMITTED) ? oldFile : newFile;
                if (fileToDelete == null) {
                    return;
                }
                try {
                    Files.deleteIfExists(fileToDelete);
                } catch (IOException e) {
                    log.warn("Could not delete file {}", fileToDelete, e);
                }
            }
        });
    }
}

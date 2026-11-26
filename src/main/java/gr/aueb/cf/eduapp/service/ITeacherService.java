package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.*;
import gr.aueb.cf.eduapp.core.filters.TeacherFilters;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ITeacherService {
    TeacherReadOnlyDTO saveTeacher(TeacherInsertDTO teacherInsertDTO)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException;

    void saveAmkaFile(UUID uuid, MultipartFile amkaFile)
            throws FileUploadException, EntityNotFoundException, InvalidFileException;

    TeacherReadOnlyDTO updateTeacher(TeacherUpdateDTO teacherUpdateDTO)
            throws EntityNotFoundException, EntityAlreadyExistsException, EntityInvalidArgumentException;

    TeacherReadOnlyDTO deleteTeacherByUUID(UUID uuid) throws EntityNotFoundException;

    TeacherReadOnlyDTO getTeacherByUUIDDeletedFalse(UUID uuid) throws EntityNotFoundException;

    Page<TeacherReadOnlyDTO> getPaginatedTeachers(Pageable pageable);

    Page<TeacherReadOnlyDTO> getPaginatedTeachersDeletedFalse(Pageable pageable);

    Page<TeacherReadOnlyDTO> getTeachersPaginatedFiltered(Pageable pageable, TeacherFilters filters)
            throws EntityNotFoundException;

    boolean isTeacherExists(String vat);

    boolean isAmkaExists(String amka);

    boolean isIdentityNumberExists(String identityNumber);
}

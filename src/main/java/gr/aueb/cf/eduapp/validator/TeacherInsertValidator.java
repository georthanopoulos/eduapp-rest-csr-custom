package gr.aueb.cf.eduapp.validator;

import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.service.ITeacherService;
import gr.aueb.cf.eduapp.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherInsertValidator implements Validator {

    private final ITeacherService teacherService;
    private final IUserService userService;

    @Override
    public boolean supports(Class<?> clazz) {
        return TeacherInsertDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TeacherInsertDTO teacherInsertDTO = (TeacherInsertDTO) target;

        if (teacherInsertDTO.vat() != null && teacherService.isTeacherExists(teacherInsertDTO.vat())) {
            log.warn("Save failed. Teacher with this vat already exists");
            errors.rejectValue("vat", "teacher.vat.exists", "Teacher with this vat already exists");
        }

        if (teacherInsertDTO.personalInfoInsertDTO() != null && teacherInsertDTO.personalInfoInsertDTO().amka() != null &&
                teacherService.isAmkaExists(teacherInsertDTO.personalInfoInsertDTO().amka())) {
            log.warn("Save failed. Teacher with this amka already exists");
            errors.rejectValue("personalInfoInsertDTO.amka", "teacher.amka.exists", "Teacher with this amka already exists");
        }

        if(teacherInsertDTO.personalInfoInsertDTO() != null && teacherInsertDTO.personalInfoInsertDTO().identityNumber() != null &&
                teacherService.isIdentityNumberExists(teacherInsertDTO.personalInfoInsertDTO().identityNumber())) {
            log.warn("Save failed. Teacher with this identity number already exists");
            errors.rejectValue("personalInfoInsertDTO.identityNumber", "teacher.identityNumber.exists", "Teacher with this identity number already exists");
        }

        if (teacherInsertDTO.userInsertDTO() != null && teacherInsertDTO.userInsertDTO().username() != null &&
                userService.isUserExists(teacherInsertDTO.userInsertDTO().username())) {
            log.warn("Save failed. Teacher with this username already exists");
            errors.rejectValue("userInsertDTO.username", "teacher.username.exists", "Teacher with this username already exists");
        }
    }
}

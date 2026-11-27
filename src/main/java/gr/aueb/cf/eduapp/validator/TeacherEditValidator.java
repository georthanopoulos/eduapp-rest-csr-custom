package gr.aueb.cf.eduapp.validator;

import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.service.ITeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherEditValidator implements Validator {

    private final ITeacherService teacherService;

    @Override
    public boolean supports(Class<?> clazz) {
        return TeacherUpdateDTO.class == clazz;
    }

    @Override
    public void validate(Object target, Errors errors) {
        TeacherUpdateDTO teacherUpdateDTO = (TeacherUpdateDTO) target;

        try {
            TeacherReadOnlyDTO readOnlyDTO = teacherService.getTeacherByUUIDDeletedFalse(teacherUpdateDTO.uuid());

            if (readOnlyDTO != null && !readOnlyDTO.vat().equals(teacherUpdateDTO.vat())) {
                if (teacherService.isTeacherExists(teacherUpdateDTO.vat())) {
                    log.warn("Update failed. Teacher with this vat already exists");
                    errors.rejectValue("vat", "teacher.vat.exists", "Teacher with this vat already exists");
                }
            }
        } catch (EntityNotFoundException e) {
            log.warn("Update failed. Teacher with uuid={} not found", teacherUpdateDTO.uuid());
            errors.rejectValue("uuid", "teacher.uuid.notfound", "Teacher with this uuid was not found");
        }
    }
}

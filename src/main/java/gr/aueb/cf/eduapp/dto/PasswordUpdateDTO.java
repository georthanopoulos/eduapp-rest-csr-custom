package gr.aueb.cf.eduapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PasswordUpdateDTO(

        @NotNull
        String oldPassword,

        @NotNull
        @Pattern(regexp = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=])^.{8,}$")
        String newPassword
) {
}
package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.dto.PasswordUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.dto.UserReadOnlyDTO;

import java.util.UUID;

public interface IUserService {
    UserReadOnlyDTO saveUser(UserInsertDTO userInsertDTO)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException;

    UserReadOnlyDTO getUserByUUID(UUID uuid) throws EntityNotFoundException;
    UserReadOnlyDTO getUserByUUIDDeletedFalse(UUID uuid) throws EntityNotFoundException;
    boolean isUserExists(String username);

    void changePassword(UUID uuid, PasswordUpdateDTO dto)    // plain "void". the public is always meant when we are on Interface-level! (would be redundant if included)! Unlike to a class case scenario when the plain "void" means PACKAGE-PRIVATE, therefore the "public" must be EXPLICITLY stated if needed.
            throws EntityNotFoundException, EntityInvalidArgumentException;
}

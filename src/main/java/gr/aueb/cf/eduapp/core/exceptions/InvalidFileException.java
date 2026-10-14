package gr.aueb.cf.eduapp.core.exceptions;

public class InvalidFileException extends AppGenericException {
    private static final String DEFAULT_CODE = "InvalidFileArgument";

    public InvalidFileException(String code, String message) {
        super(code + DEFAULT_CODE, message);
    }
}

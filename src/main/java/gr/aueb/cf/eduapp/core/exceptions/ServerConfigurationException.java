package gr.aueb.cf.eduapp.core.exceptions;

public class ServerConfigurationException extends AppGenericException {

    private static final String DEFAULT_CODE = "ServerConfiguration";

    public ServerConfigurationException(String code, String message) {
        super(code + DEFAULT_CODE, message);
    }
}

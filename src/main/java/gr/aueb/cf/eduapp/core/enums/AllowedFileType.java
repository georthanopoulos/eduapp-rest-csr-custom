package gr.aueb.cf.eduapp.core.enums;

import java.util.Optional;

public enum AllowedFileType {

    PDF("application/pdf", ".pdf"),                      // pattern -> constant("contentType", "extension")
    PNG("image/png", ".png"),
    JPEG("image/jpeg", ".jpg");

    private final String contentType;    // field 1
    private final String extension;        // field 2

    AllowedFileType(String contentType, String extension) {   // constructor -> JAVA calls it for every given constant.
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {              // getter, in order to read the value externally
        return contentType;
    }

    public String getExtension() {                 // getter, in order to read the value externally
        return extension;
    }

    public static Optional<AllowedFileType> getByContentType(String contentType) {
        for (AllowedFileType allowedFileType : AllowedFileType.values()) {
            if (allowedFileType.getContentType().equals(contentType)) {
                return Optional.of(allowedFileType);
            }
        }
        return Optional.empty();
    }
}

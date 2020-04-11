package kirill.subtitlemerger.logic.utils.file_validation;

public enum InputFileNotValidReason {
    PATH_IS_EMPTY,
    PATH_IS_TOO_LONG,
    INVALID_PATH,
    IS_A_DIRECTORY,
    DOES_NOT_EXIST,
    NO_EXTENSION,
    NOT_ALLOWED_EXTENSION,
    FILE_IS_EMPTY,
    FILE_IS_TOO_BIG,
    FAILED_TO_READ_CONTENT
}

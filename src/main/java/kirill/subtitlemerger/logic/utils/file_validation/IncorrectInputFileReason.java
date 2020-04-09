package kirill.subtitlemerger.logic.utils.file_validation;

public enum IncorrectInputFileReason {
    PATH_IS_TOO_LONG,
    INVALID_PATH,
    IS_A_DIRECTORY,
    DOES_NOT_EXIST,
    FAILED_TO_GET_PARENT_DIRECTORY,
    EXTENSION_IS_NOT_VALID,
    FILE_IS_EMPTY,
    FILE_IS_TOO_BIG,
    FAILED_TO_READ_CONTENT
}

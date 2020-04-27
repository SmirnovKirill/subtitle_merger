package kirill.subtitlemerger.gui;

import javafx.stage.FileChooser;

public class GuiConstants {
    public static final boolean GRID_LINES_VISIBLE = false;

    public static final int TABLE_FILE_LIMIT = 10_000;

    public static final FileChooser.ExtensionFilter VIDEO_EXTENSION_FILTER = new FileChooser.ExtensionFilter(
            "mkv files (*.mkv)", "*.mkv"
    );

    public static final FileChooser.ExtensionFilter SUB_RIP_EXTENSION_FILTER = new FileChooser.ExtensionFilter(
            "subrip files (*.srt)", "*.srt"
    );

    public static final String BUTTON_ERROR_CLASS = "button-error";

    public static final String TEXT_FIELD_ERROR_CLASS = "text-field-error";

    public static final String LABEL_SUCCESS_CLASS = "label-success";

    public static final String LABEL_WARN_CLASS = "label-warn";

    public static final String LABEL_ERROR_CLASS = "label-error";

    public static final String IMAGE_BUTTON_CLASS = "image-button";
}
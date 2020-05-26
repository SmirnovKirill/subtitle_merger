package kirill.subtitlemerger.gui;

import javafx.stage.FileChooser;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitleFormat;

public class GuiConstants {
    public static final boolean GRID_LINES_VISIBLE = false;

    public static final int VIDEO_TABLE_LIMIT = 10_000;

    public static final FileChooser.ExtensionFilter VIDEO_EXTENSION_FILTER = new FileChooser.ExtensionFilter(
            "mkv files (*.mkv)", "*.mkv"
    );

    public static final FileChooser.ExtensionFilter SUBTITLE_EXTENSION_FILTER = getSubtitleExtensionFilter();

    /**
     * If a video file for which subtitles are being loaded has a size in bytes equal to or greater than this number
     * then there will be a message before the cancel link saying that the operation may take a while.
     */
    public static final long LOAD_CANCEL_DESCRIPTION_THRESHOLD = 1024 * 1024 * 1024L;

    /**
     * If a video file for which subtitles are being injected has a size in bytes equal to or greater than this number
     * then there will be a message before the cancel link saying that the operation may take a while.
     */
    public static final long MERGE_CANCEL_DESCRIPTION_THRESHOLD = 1024 * 1024 * 1024L;

    public static final String BUTTON_ERROR_CLASS = "button-error";

    public static final String TEXT_FIELD_ERROR_CLASS = "text-field-error";

    public static final String LABEL_SUCCESS_CLASS = "label-success";

    public static final String LABEL_WARNING_CLASS = "label-warning";

    public static final String LABEL_ERROR_CLASS = "label-error";

    public static final String IMAGE_BUTTON_CLASS = "image-button";

    private static FileChooser.ExtensionFilter getSubtitleExtensionFilter() {
        String subRipExtension = SubtitleFormat.SUB_RIP.getExtensions().get(0);

        return new FileChooser.ExtensionFilter(
                "SubRip files (*." + subRipExtension +  ")", "*." + subRipExtension
        );
    }
}
package kirill.subtitlesmerger.logic;

import java.util.Collections;
import java.util.List;

public class Constants {
    public static final String PREFERENCES_ROOT_NODE = "subtitlesmerger";

    public static final List<String> ALLOWED_VIDEO_EXTENSIONS = Collections.singletonList("mkv");

    public static final List<String> ALLOWED_VIDEO_MIME_TYPES = Collections.singletonList("video/x-matroska");

    public static final boolean DEBUG = false;
}

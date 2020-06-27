package kirill.subtitlemerger.logic.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@AllArgsConstructor
@Getter
public enum SettingType {
    LAST_DIRECTORY_WITH_UPPER_SUBTITLES("last_directory_with_upper_subtitles", File.class),
    LAST_DIRECTORY_WITH_LOWER_SUBTITLES("last_directory_with_lower_subtitles", File.class),
    LAST_DIRECTORY_WITH_MERGED_SUBTITLES("last_directory_with_merged_subtitles", File.class),
    UPPER_LANGUAGE("upper_language", LanguageAlpha3Code.class),
    LOWER_LANGUAGE("lower_language", LanguageAlpha3Code.class),
    MERGE_MODE("merge_mode", MergeMode.class),
    MAKE_MERGED_STREAMS_DEFAULT("make_merged_streams_default", Boolean.class),
    PLAIN_TEXT_SUBTITLES("plain_text_subtitles", Boolean.class),
    LAST_DIRECTORY_WITH_VIDEOS("last_directory_with_videos", File.class),
    LAST_DIRECTORY_WITH_VIDEO_SUBTITLES("last_directory_with_video_subtitles", File.class),
    SORT_BY("sort_by", SortBy.class),
    SORT_DIRECTION("sort_direction", SortDirection.class);

    private String code;

    private Class objectClass;
}

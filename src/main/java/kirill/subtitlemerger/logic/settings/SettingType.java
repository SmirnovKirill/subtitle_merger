package kirill.subtitlemerger.logic.settings;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@AllArgsConstructor
@Getter
public enum SettingType {
    UPPER_DIRECTORY("last_directory_with_upper_subtitles", File.class),
    LOWER_DIRECTORY("last_directory_with_lower_subtitles", File.class),
    MERGED_DIRECTORY("last_directory_with_merged_subtitles", File.class),
    UPPER_LANGUAGE("upper_subtitles_language", LanguageAlpha3Code.class),
    LOWER_LANGUAGE("lower__subtitles_language", LanguageAlpha3Code.class),
    MERGE_MODE("merge_mode", MergeMode.class),
    MAKE_MERGED_STREAMS_DEFAULT("make_merged_streams_default", Boolean.class),
    PLAIN_TEXT_SUBTITLES("plain_text_subtitles", Boolean.class),
    VIDEO_DIRECTORY("last_directory_with_videos", File.class),
    VIDEO_SUBTITLE_DIRECTORY("last_directory_with_video_subtitles", File.class),
    SORT_BY("sort_by", SortBy.class),
    SORT_DIRECTION("sort_direction", SortDirection.class);

    private String code;

    private Class objectClass;
}

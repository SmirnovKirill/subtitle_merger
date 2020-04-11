package kirill.subtitlemerger.logic.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SettingType {
    UPPER_SUBTITLES_LAST_DIRECTORY("upper_subtitles_last_directory"),
    LOWER_SUBTITLES_LAST_DIRECTORY("lower_subtitles_last_directory"),
    MERGED_SUBTITLES_LAST_DIRECTORY("merged_subtitles_last_directory"),
    FFPROBE_PATH("ffprobe_path"),
    FFMPEG_PATH("ffmpeg_path"),
    UPPER_LANGUAGE("upper_language"),
    LOWER_LANGUAGE("lower_language"),
    MERGE_MODE("merge_mode"),
    MARK_MERGED_STREAM_AS_DEFAULT("mark_merged_stream_as_default"),
    LAST_DIRECTORY_WITH_VIDEOS("last_directory_with_videos"),
    SORT_BY("sort_by"),
    SORT_DIRECTION("sort_direction"),
    LAST_DIRECTORY_WITH_EXTERNAL_SUBTITLES("last_directory_with_external_subtitles");

    private String code;
}

package kirill.subtitlemerger.logic.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SettingType {
    UPPER_DIRECTORY("last_directory_with_upper_subtitles"),
    LOWER_DIRECTORY("last_directory_with_lower_subtitles"),
    MERGED_DIRECTORY("last_directory_with_merged_subtitles"),
    FFPROBE_PATH("ffprobe_path"),
    FFMPEG_PATH("ffmpeg_path"),
    UPPER_LANGUAGE("upper_subtitles_language"),
    LOWER_LANGUAGE("lower__subtitles_language"),
    MERGE_MODE("merge_mode"),
    MAKE_MERGED_STREAMS_DEFAULT("make_merged_streams_default"),
    VIDEOS_DIRECTORY("last_directory_with_videos"),
    SORT_BY("sort_by"),
    SORT_DIRECTION("sort_direction"),
    EXTERNAL_DIRECTORY("last_directory_with_external_subtitles");

    private String code;
}

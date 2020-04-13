package kirill.subtitlemerger.logic.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SettingType {
    UPPER_SUBTITLES_DIRECTORY("upper_subtitles_directory"),
    LOWER_SUBTITLES_DIRECTORY("lower_subtitles_directory"),
    MERGED_SUBTITLES_DIRECTORY("merged_subtitles_directory"),
    FFPROBE_PATH("ffprobe_path"),
    FFMPEG_PATH("ffmpeg_path"),
    UPPER_LANGUAGE("upper_language"),
    LOWER_LANGUAGE("lower_language"),
    MERGE_MODE("merge_mode"),
    MAKE_MERGED_STREAMS_DEFAULT("make_merged_streams_default"),
    VIDEOS_DIRECTORY("directory_with_videos"),
    SORT_BY("sort_by"),
    SORT_DIRECTION("sort_direction"),
    EXTERNAL_SUBTITLES_DIRECTORY("external_subtitles_directory");

    private String code;
}

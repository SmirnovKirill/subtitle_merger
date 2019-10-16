package kirill.subtitles_merger;

public enum FullFileUnavailabilityReason {
    FAILED_BEFORE, // Значит упало раньше, на предыдущем этапе, когда получали краткую информацию.
    FFMPEG_FAILED
}

package kirill.subtitles_merger;

public enum FullFileUnavailabilityReason {
    FAILED_BEFORE, // means that an error has happened before, at previous stage when we were obtaining brief info
    FFMPEG_FAILED
}

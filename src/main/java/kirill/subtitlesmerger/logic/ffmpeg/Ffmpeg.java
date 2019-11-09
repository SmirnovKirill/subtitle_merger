package kirill.subtitlesmerger.logic.ffmpeg;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.data.Subtitles;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
public class Ffmpeg {
    private static final File TEMP_SUBTITLE_FILE = new File(
            System.getProperty("java.io.tmpdir"),
            "subtitles_merger_temp.srt"
    );

    private String path;

    public Ffmpeg(String path) throws FfmpegException {
        validate(path);

        this.path = path;
    }

    public static void validate(String ffmpegPath) throws FfmpegException {
        try {
            String consoleOutput = ProcessRunner.run(
                    Arrays.asList(
                            ffmpegPath,
                            "-version"
                    )
            );

            if (!consoleOutput.startsWith("ffmpeg version")) {
                log.info("console output doesn't start with the ffmpeg version");
                throw new FfmpegException(FfmpegException.Code.INCORRECT_FFMPEG_PATH);
            }
        } catch (ProcessException e) {
            log.info("failed to check ffmpeg: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.INCORRECT_FFMPEG_PATH);
        }
    }

    /*
     * Synchronized because we use one temporary file with subtitles.
     */
    public synchronized String getSubtitlesText(int streamIndex, File videoFile) throws FfmpegException {
        try {
            ProcessRunner.run(
                    /*
                     * We have to pass -y to agree with file overwriting, it's always required
                     * because java will have created temporary file by the time ffmpeg is called.
                     */
                    Arrays.asList(
                            path,
                            "-y",
                            "-i",
                            videoFile.getAbsolutePath(),
                            "-map",
                            "0:" + streamIndex,
                            TEMP_SUBTITLE_FILE.getAbsolutePath()
                    )
            );
        } catch (ProcessException e) {
            log.warn("failed to extract subtitles with ffmpeg: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
        }

        try {
            return FileUtils.readFileToString(TEMP_SUBTITLE_FILE);
        } catch (IOException e) {
            log.warn("failed to read subtitles from the file: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
        }
    }

    /*
     * Synchronized because we use one temporary file with subtitles.
     */
    public synchronized void injectSubtitlesToFile(
            Subtitles subtitles,
            int subtitlesStreamsAmount,
            File videoFile
    ) throws FfmpegException {
        /*
         * Ffmpeg can't add subtitles on the fly. So we need to add subtitles to some temporary file
         * and then rename it. Later we'll also check that the size of the new file is bigger than the size of the
         * original one because it's important not to spoil the original video file, it may be valuable.
         */
        File outputTemp = new File(videoFile.getParentFile(), "temp_" + videoFile.getName());

        try {
            FileUtils.writeStringToFile(TEMP_SUBTITLE_FILE, subtitles.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("failed to write merged subtitles to the temp file: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
        }

        LanguageAlpha3Code language = null;
        if (!CollectionUtils.isEmpty(subtitles.getLanguages())) {
            language = subtitles.getLanguages().get(0);
        }

        try {
            try {
                ProcessRunner.run(
                        getArgumentsInjectToFile(
                                videoFile,
                                outputTemp,
                                subtitlesStreamsAmount,
                                language
                        )
                );
            } catch (ProcessException e) {
                log.warn("failed to inject subtitles with ffmpeg: " + e.getCode());
                throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
            }

            if (outputTemp.length() <= videoFile.length()) {
                log.warn("resulting file size is less than the original one");
                throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
            }

            overwriteOriginalVideo(outputTemp, videoFile);
        } finally {
            if (outputTemp.exists() && !outputTemp.delete()) {
                log.warn("failed to delete temp video file " + outputTemp.getAbsolutePath());
            }
        }
    }

    private List<String> getArgumentsInjectToFile(
            File videoFile,
            File outputTemp,
            int subtitlesStreamsAmount,
            LanguageAlpha3Code language
    ) {
        List<String> result = new ArrayList<>();

        result.add(path);
        result.add("-y");

        /*
         * Haven't seen such scenario in practice but I've looked this argument up in the ffmpeg documentation
         * and decided that it's worth adding. Because if there are some unknown streams in the file it's better
         * to add them as is, our program is designed just for injecting subtitles, leave everything else untouched.
         */
        result.add("-copy_unknown");

        result.addAll(Arrays.asList("-i", videoFile.getAbsolutePath()));
        result.addAll(Arrays.asList("-i", Ffmpeg.TEMP_SUBTITLE_FILE.getAbsolutePath()));
        result.addAll(Arrays.asList("-c", "copy"));

        /*
         * Very important! Without this argument many videos won't work:
         * https://video.stackexchange.com/questions/28719/srt-subtitles-added-to-mkv-with-ffmpeg-are-not-displayed
         */
        result.addAll(Arrays.asList("-max_interleave_delta", "0"));

        if (language != null) {
            result.addAll(Arrays.asList("-metadata:s:s:" + subtitlesStreamsAmount, "language=" + language.toString()));
        }
        result.addAll(Arrays.asList("-metadata:s:s:" + subtitlesStreamsAmount, "title=Merged subtitles"));
        result.addAll(Arrays.asList("-disposition:s:" + subtitlesStreamsAmount, "default"));
        result.addAll(Arrays.asList("-map", "0"));
        result.addAll(Arrays.asList("-map", "1"));
        result.add(outputTemp.getAbsolutePath());

        return result;
    }

    private static void overwriteOriginalVideo(File outputTemp, File videoFile) throws FfmpegException {
        /*
         * Save this flag here to restore it at the end of the method. Because otherwise if the file has had only
         * read access initially we will give it write access as well before renaming, and leave it like that.
         */
        boolean originallyWritable = videoFile.canWrite();

        if (!videoFile.setWritable(true, true)) {
            log.warn("failed to make video file " + videoFile.getAbsolutePath() + " writable");
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
        }

        try {
            Files.move(outputTemp.toPath(), videoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("failed to move temp video: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.FAILED_TO_MOVE_TEMP_VIDEO);
        }

        if (!originallyWritable) {
            if (!videoFile.setWritable(false, true)) {
                log.warn("failed to make video file " + videoFile.getAbsolutePath() + " not writable");
            }
        }
    }
}

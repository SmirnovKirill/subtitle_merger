package kirill.subtitlemerger.logic.ffmpeg;

import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.utils.process.ProcessException;
import kirill.subtitlemerger.logic.utils.process.ProcessRunner;
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
import java.util.Objects;

@CommonsLog
public class Ffmpeg {
    private static final File TEMP_SUBTITLE_FILE = new File(
            System.getProperty("java.io.tmpdir"),
            "subtitle_merger_temp.srt"
    );

    private File ffmpegFile;

    public Ffmpeg(File ffmpegFile) throws FfmpegException, InterruptedException {
        validate(ffmpegFile);

        this.ffmpegFile = ffmpegFile;
    }

    private static void validate(File ffmpegFile) throws FfmpegException, InterruptedException {
        try {
            List<String> arguments = Arrays.asList(
                    ffmpegFile.getAbsolutePath(),
                    "-version"
            );

            String consoleOutput = ProcessRunner.run(arguments);
            if (!consoleOutput.startsWith("ffmpeg version")) {
                throw new FfmpegException(FfmpegException.Code.INCORRECT_FFMPEG_PATH, consoleOutput);
            }
        } catch (ProcessException e) {
            throw new FfmpegException(FfmpegException.Code.INCORRECT_FFMPEG_PATH, e.getConsoleOutput());
        }
    }

    /*
     * Synchronized because we use one temporary file with subtitles.
     */
    public synchronized byte[] getSubtitles(
            int ffmpegStreamIndex,
            String format,
            File videoFile
    ) throws FfmpegException, InterruptedException {
        String consoleOutput;
        try {
            List<String> arguments = new ArrayList<>(
                    Arrays.asList(
                            ffmpegFile.getAbsolutePath(),
                            "-y",
                            "-i",
                            videoFile.getAbsolutePath(),
                            "-map",
                            "0:" + ffmpegStreamIndex
                    )
            );
            if (Objects.equals(format, LogicConstants.SUB_STATION_ALPHA_FORMAT)) {
                arguments.addAll(Arrays.asList("-c", "srt"));
            }
            arguments.add(TEMP_SUBTITLE_FILE.getAbsolutePath());

            consoleOutput = ProcessRunner.run(arguments);
        } catch (ProcessException e) {
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, e.getConsoleOutput());
        }

        try {
            return FileUtils.readFileToByteArray(TEMP_SUBTITLE_FILE);
        } catch (IOException e) {
            log.warn("failed to read subtitles from video: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, consoleOutput);
        }
    }

    /*
     * Synchronized because we use one temporary file with subtitles.
     */
    public synchronized void injectSubtitlesToFile(
            FfmpegInjectInfo injectInfo
    ) throws FfmpegException, InterruptedException {
        /*
         * Ffmpeg can't add subtitles on the fly. So we need to add subtitles to some temporary file
         * and then rename it.
         */
        File tempVideoFile = new File(
                injectInfo.getTempVideoDirectory(),
                "temp_" + injectInfo.getOriginalVideoFile().getName()
        );

        try {
            FileUtils.writeStringToFile(TEMP_SUBTITLE_FILE, injectInfo.getSubtitles(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("failed to write the merged subtitles to the temporary file: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, null);
        }

        try {
            String consoleOutput;
            try {
                List<String> arguments = getArgumentsInjectToFile(injectInfo, tempVideoFile);
                consoleOutput = ProcessRunner.run(arguments);
            } catch (ProcessException e) {
                throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, e.getConsoleOutput());
            }

            overwriteOriginalVideo(tempVideoFile, injectInfo.getOriginalVideoFile(), consoleOutput);
        } finally {
            if (tempVideoFile.exists() && !tempVideoFile.delete()) {
                log.warn("failed to delete the temporary video file " + tempVideoFile.getAbsolutePath());
            }
        }
    }

    private List<String> getArgumentsInjectToFile(FfmpegInjectInfo injectInfo, File tempVideoFile) {
        List<String> result = new ArrayList<>();

        result.add(ffmpegFile.getAbsolutePath());
        result.add("-y");

        /*
         * Haven't seen such scenario in practice but I've looked this argument up in the ffmpeg documentation
         * and decided that it's worth adding. Because if there are some unknown streams in the file it's better
         * to add them as is, our program is designed just for injecting subtitles, leave everything else untouched.
         */
        result.add("-copy_unknown");

        result.addAll(Arrays.asList("-i", injectInfo.getOriginalVideoFile().getAbsolutePath()));
        result.addAll(Arrays.asList("-i", Ffmpeg.TEMP_SUBTITLE_FILE.getAbsolutePath()));
        result.addAll(Arrays.asList("-c", "copy"));

        /*
         * Very important! Without this argument many videos won't work:
         * https://video.stackexchange.com/questions/28719/srt-subtitles-added-to-mkv-with-ffmpeg-are-not-displayed
         */
        result.addAll(Arrays.asList("-max_interleave_delta", "0"));

        int newStreamIndex = injectInfo.getCurrentSubtitleCount();

        if (injectInfo.getLanguage() != null) {
            result.add("-metadata:s:s:" + newStreamIndex);
            result.add("language=" + injectInfo.getLanguage());
        }

        result.add("-metadata:s:s:" + newStreamIndex);
        result.add("title=" + injectInfo.getTitle());

        if (injectInfo.isMakeDefault()) {
            if (!CollectionUtils.isEmpty(injectInfo.getStreamsToMakeNotDefaultIndices())) {
                for (int index : injectInfo.getStreamsToMakeNotDefaultIndices()) {
                    result.addAll(Arrays.asList("-disposition:" + index, "0"));
                }
            }

            result.addAll(Arrays.asList("-disposition:s:" + newStreamIndex, "default"));
        }

        result.addAll(Arrays.asList("-map", "0"));
        result.addAll(Arrays.asList("-map", "1"));
        result.add(tempVideoFile.getAbsolutePath());

        return result;
    }

    private static void overwriteOriginalVideo(
            File tempVideoFile,
            File originalVideoFile,
            String consoleOutput
    ) throws FfmpegException {
        /*
         * Save this flag here to restore it at the end of the method. Because otherwise if the file has had only
         * read access initially we will give it the write access as well before renaming, and leave it like that.
         */
        boolean originallyWritable = originalVideoFile.canWrite();

        if (!originalVideoFile.setWritable(true, true)) {
            log.warn("failed to make video file " + originalVideoFile.getAbsolutePath() + " writable");
            throw new FfmpegException(FfmpegException.Code.FAILED_TO_MOVE_TEMP_VIDEO, consoleOutput);
        }

        try {
            Files.move(tempVideoFile.toPath(), originalVideoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("failed to move temporary video: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.FAILED_TO_MOVE_TEMP_VIDEO, consoleOutput);
        }

        if (!originallyWritable) {
            if (!originalVideoFile.setWritable(false, true)) {
                log.warn("failed to make video file " + originalVideoFile.getAbsolutePath() + " not writable");
            }
        }
    }
}

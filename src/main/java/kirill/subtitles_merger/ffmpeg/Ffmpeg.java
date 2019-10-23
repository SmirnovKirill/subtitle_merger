package kirill.subtitles_merger.ffmpeg;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitles_merger.logic.Subtitles;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
public class Ffmpeg {
    private static final File TEMP_SUBTITLE_FILE = new File(System.getProperty("java.io.tmpdir"), "subtitles_merger_temp.srt");
    private String path;

    public Ffmpeg(String path) {
        this.path = path;
        validate();
    }

    private void validate() {

    }

    public synchronized String getSubtitlesText(int streamIndex, File videoFile) throws FfmpegException, InterruptedException {
        try {
            /*
             * -y нужно передавать чтобы дать согласие на перезаписывание файла, это всегда нужно делать потому что временный
             * файл уже будет создан джавой на момент вызова ffmpeg.
             */
            ProcessBuilder processBuilder = new ProcessBuilder(
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
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("ffmpeg exited with code " + exitCode + ": " + output.toString());
                throw new IllegalStateException();
            }

            return FileUtils.readFileToString(TEMP_SUBTITLE_FILE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new FfmpegException(FfmpegException.Code.COMMON, e);
        }
    }

    //todo переделать, пока наспех
    public void addSubtitleToFile(
            Subtitles subtitles,
            int existingSubtitlesLength,
            File videoFile
    ) throws InterruptedException, IOException {
        /*
         * Ffmpeg не может добавить к файлу субтитры и записать это в тот же файл.
         * Поэтому нужно сначала записать результат во временный файл а потом его переименовать.
         * На всякий случай еще сделаем проверку что новый файл больше чем старый, а то нехорошо будет если испортим
         * видео, его могли долго качать.
         */
        File outputTemp = new File(videoFile.getParentFile(), "temp_" + videoFile.getName());

        File subtitlesTemp = File.createTempFile("subtitles_merger_", ".srt");
        FileUtils.writeStringToFile(subtitlesTemp, subtitles.toString(), StandardCharsets.UTF_8);

        LanguageAlpha3Code language = null;
        if (!CollectionUtils.isEmpty(subtitles.getLanguages())) {
            language = subtitles.getLanguages().get(0);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                getArgumentAddToFile(
                        videoFile,
                        subtitlesTemp,
                        outputTemp,
                        existingSubtitlesLength,
                        language
                )
        );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("ffmpeg exited with code " + exitCode + ": " + output.toString());
            throw new IllegalStateException();
        }

        if (outputTemp.length() <= videoFile.length()) {
            log.error("resulting file size is less than original one");
            throw new IllegalStateException();
        }

    }

    private List<String> getArgumentAddToFile(
            File videoFile,
            File subtitlesTemp,
            File outputTemp,
            int existingSubtitlesLength,
            LanguageAlpha3Code language
    ) {
        List<String> result = new ArrayList<>();

        result.add(path);
        result.add("-y");

        /*
         * Не сталкивался с таким, но прочел в документации ffmpeg и мне показалось что стоит добавить.
         * Потому что вдруг есть какие то неизвестные стримы в файле, пусть они без изменений копируются,
         * потому что задача метода просто добавить субтитры не меняя ничего другого.
         */
        result.add("-copy_unknown");

        result.addAll(Arrays.asList("-i", videoFile.getAbsolutePath()));
        result.addAll(Arrays.asList("-i", subtitlesTemp.getAbsolutePath()));
        result.addAll(Arrays.asList("-c", "copy"));

        /*
         * Очень важный аргумент, без него во многих видео может быть проблема.
         * https://video.stackexchange.com/questions/28719/srt-subtitles-added-to-mkv-with-ffmpeg-are-not-displayed
         */
        result.addAll(Arrays.asList("-max_interleave_delta", "0"));

        if (language != null) {
            result.addAll(Arrays.asList("-metadata:s:s:" + existingSubtitlesLength, "language=" + language.toString()));
        }
        result.addAll(Arrays.asList("-metadata:s:s:" + existingSubtitlesLength, "title=Merged subtitles"));
        result.addAll(Arrays.asList("-disposition:s:s:" + existingSubtitlesLength, "default"));
        result.addAll(Arrays.asList("-map", "0"));
        result.addAll(Arrays.asList("-map", "1"));
        result.add(outputTemp.getAbsolutePath());

        return result;
    }
}

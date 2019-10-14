package kirill.subtitles_merger;

import kirill.subtitles_merger.logic.Merger;
import kirill.subtitles_merger.logic.Parser;
import kirill.subtitles_merger.logic.Subtitles;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Main {
    private static final String PATH_TO_UPPER_SUBTITLES_PROPERTY = "pathToUpperSubtitles";

    private static final String PATH_TO_LOWER_SUBTITLES_PROPERTY = "pathToLowerSubtitles";

    private static final String PATH_TO_MERGED_SUBTITLES_PROPERTY = "pathToMergedSubtitles";

    public static void main(String[] args) throws IOException {
        Properties properties = getProperties();

        Subtitles upperSubtitles = Parser.parseSubtitles(properties.getProperty(PATH_TO_UPPER_SUBTITLES_PROPERTY), "upper");
        Subtitles lowerSubtitles = Parser.parseSubtitles(properties.getProperty(PATH_TO_LOWER_SUBTITLES_PROPERTY), "lower");

        Subtitles mergedSubtitles = Merger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        FileUtils.writeStringToFile(new File(properties.getProperty(PATH_TO_MERGED_SUBTITLES_PROPERTY)), mergedSubtitles.toString());
    }

    private static Properties getProperties() throws IOException {
        Properties result = new Properties();

        result.load(Merger.class.getResourceAsStream("/config.properties"));

        return result;
    }
}

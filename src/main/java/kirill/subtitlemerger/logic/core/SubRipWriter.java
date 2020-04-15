package kirill.subtitlemerger.logic.core;

import kirill.subtitlemerger.logic.LogicConstants;
import kirill.subtitlemerger.logic.core.entities.Subtitle;
import kirill.subtitlemerger.logic.core.entities.Subtitles;

public class SubRipWriter {
    public static String toText(Subtitles subtitles, boolean plainText) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < subtitles.getSubtitles().size(); i++) {
            Subtitle subtitle = subtitles.getSubtitles().get(i);

            result.append(i + 1);
            result.append(System.lineSeparator());

            result.append(LogicConstants.SUBRIP_TIME_FORMATTER.print(subtitle.getFrom()));
            result.append(" --> ");
            result.append(LogicConstants.SUBRIP_TIME_FORMATTER.print(subtitle.getTo()));
            result.append(System.lineSeparator());

            for (int j = 0; j < subtitle.getLines().size(); j++) {
                if (plainText) {
                    result.append(getPlainText(subtitle.getLines().get(j)));
                } else {
                    result.append(subtitle.getLines().get(j));
                }

                if (j != subtitle.getLines().size() - 1 || i != subtitles.getSubtitles().size() - 1) {
                    result.append(System.lineSeparator());
                }
            }

            if (i != subtitles.getSubtitles().size() - 1) {
                result.append(System.lineSeparator());
            }
        }

        return result.toString();
    }

    /**
     * This method removes HTML and ASS tags using regular expressions. I realize that it's not right to work with HTML
     * using regular expressions and I could have used jsoup to clear tags but here are the arguments not to do it:
     * 1) Subtitle text is not an html text, it's a regular text with html tags as far as I know. So for example string
     * "<test" is correct in terms of subtitles but incorrect in terms of HTML because brackets that are not tags have
     * to be encoded. So if I use jsoup it will simply remove the text "<test" completely instead of leaving it as is.
     * 2) All not removed basic html-entities will be html encoded so I have to decode them back.
     * 3) Even if I use jsoup there are no well-known libraries for removing ASS tags.
     * 4) Regular expressions are faster than jsoup.
     * 5) If it's possible not to use another library it's better to do so to reduce the jar's size.
     * So I think in general for our purposes regular expressions will suffice.
     */
    private static String getPlainText(String text) {
        String result = text;
        if (text.contains("<") || text.contains(">")) {
            result = result.replaceAll("<.+?>", "");
        }
        if (text.contains("{") || text.contains("}")) {
            result = result.replaceAll("\\{.+?}", "");
        }

        return result;
    }
}

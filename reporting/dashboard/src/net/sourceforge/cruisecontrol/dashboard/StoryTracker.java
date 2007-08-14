package net.sourceforge.cruisecontrol.dashboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class StoryTracker {

    private final String projectName;

    private final String baseUrl;

    private final String keywords;

    private Pattern pattern;

    public StoryTracker(String projectName, String baseUrl, String keywords) {
        this.projectName = projectName;
        this.baseUrl = baseUrl;
        this.keywords = keywords;
        pattern = pattern();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getProjectName() {
        return projectName;
    }

    private Pattern pattern() {
        String chompedKeywords = StringUtils.chomp(keywords, ",");
        String preGroup = "(.*?)";
        String keywordsGroup = '(' + StringUtils.replaceChars(chompedKeywords, ',', '|') + ')';
        String spacesGroup = "([\t ]*)";
        String numberGroup = "(\\d+)";
        String postGroup = preGroup;
        return Pattern.compile(preGroup + keywordsGroup + spacesGroup + numberGroup + postGroup);
    }

    public String getTextWithUrls(String inputString) {
        Matcher matcher = pattern.matcher(inputString);
        if (!matcher.matches()) {
            return inputString;
        }

        matcher.reset();

        String tail = "";
        String resultString = "";
        while (matcher.find()) {
            String replacement = matcher.group(2) + matcher.group(3) + matcher.group(4);
            resultString +=
                    StringUtils.replace(matcher.group(0), replacement, "<a href=\"" + baseUrl
                            + matcher.group(4) + "\">" + replacement + "</a>");
            tail = inputString.substring(matcher.end());
        }
        resultString += tail;
        return resultString;
    }
}

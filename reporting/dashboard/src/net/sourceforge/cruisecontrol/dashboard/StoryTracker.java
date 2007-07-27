package net.sourceforge.cruisecontrol.dashboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

public class StoryTracker {

    private final String projectName;

    private final String baseUrl;

    private final String keywords;

    public StoryTracker(String projectName, String baseUrl, String keywords) {
        this.projectName = projectName;
        this.baseUrl = baseUrl;
        this.keywords = keywords;
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

    public String getStoryURL(String text) {
        int storyNumber = getStoryNumber(text);
        if (storyNumber == -1) {
            return "";
        } else {
            return baseUrl + storyNumber;
        }
    }

    private int getStoryNumber(String text) {
        String chomped = StringUtils.chomp(keywords, ",");
        String regStr = '(' + StringUtils.replaceChars(chomped, ',', '|') + ')';
        Pattern pattern = Pattern.compile(".*" + regStr + "(\\d+).*");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return NumberUtils.toInt(matcher.group(2));
        }
        return -1;
    }
}

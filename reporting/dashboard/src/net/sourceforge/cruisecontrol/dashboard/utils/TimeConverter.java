package net.sourceforge.cruisecontrol.dashboard.utils;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

public class TimeConverter {
    static final int HOUR_IN_SECONDS = 60 * 60;

    static final int DAY_IN_SECONDS = 24 * 60 * 60;

    static final int MONTH_IN_SECONDS = 30 * DAY_IN_SECONDS;

    static final int YEAR_IN_SECONDS = 365 * DAY_IN_SECONDS;

    static final String OVER_2_YEARS = "over $time years ago";

    static final String ABOUT_1_YEAR = "about 1 year ago";

    static final String FROM_2_TO_12_MONTHS = "$time months ago";

    static final String ABOUT_1_MONTH = "about 1 month ago";

    static final String FROM_2_TO_29_DAYS = "$time days ago";

    static final String ABOUT_1_DAY = "1 day ago";

    static final String ABOUT_2_TO_24_HOURS = "about $time hours ago";

    static final String ABOUT_1_HOUR = "about 1 hour ago";

    static final String FROM_2_TO_44_MINUTES = "$time minutes ago";

    static final String FROM_1_MINUTE = "1 minute ago";

    static final String LESS_THAN_A_MINUTE = "less than a minute ago";

    private static final LinkedHashMap RULES = new LinkedHashMap();

    static {
        RULES.put(Seconds.seconds(29), new TimeConverter.LessThanAMinute());
        RULES.put(Minutes.minutes(1).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.AboutOneMinute());
        RULES.put(Minutes.minutes(44).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.From2To44Minutes());
        RULES.put(Minutes.minutes(89).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.AboutOneHour());
        RULES.put(Hours.hours(23).toStandardMinutes().plus(Minutes.minutes(59)).toStandardSeconds().plus(
                Seconds.seconds(29)), new TimeConverter.About2To24Hours());
        RULES.put(Hours.hours(47).toStandardMinutes().plus(Minutes.minutes(59)).toStandardSeconds().plus(
                Seconds.seconds(29)), new TimeConverter.AboutOneDay());
        RULES.put(Days.days(29).toStandardHours().plus(Hours.hours(23)).toStandardMinutes().plus(
                Minutes.minutes(59)).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.From2To29Days());
        RULES.put(Days.days(59).toStandardHours().plus(Hours.hours(23)).toStandardMinutes().plus(
                Minutes.minutes(59)).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.AboutOneMonth());
        RULES.put(Days.days(365).toStandardSeconds().minus(Seconds.seconds(31)),
                new TimeConverter.From2To12Month());
        RULES.put(Days.days(730).toStandardSeconds().minus(Seconds.seconds(31)),
                new TimeConverter.AboutOneYear());
    }

    public String getConvertedTime(long duration) {
        Set keys = RULES.keySet();
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            Seconds seconds = (Seconds) iterator.next();
            ConvertedTime converted = (ConvertedTime) RULES.get(seconds);
            if (duration <= seconds.getSeconds()) {
                return converted.getConvertedTime(duration);
            }
        }
        return new TimeConverter.OverTwoYears().getConvertedTime(duration);
    }

    public String getConvertedTime(Date dateFrom) {
        return getConvertedTime(dateFrom, new Date());
    }

    public String getConvertedTime(Date dateLogFileGenerated, Date dateCheckTheDuration) {
        if (dateCheckTheDuration.getTime() < dateLogFileGenerated.getTime()) {
            return CCDateFormatter.getDateStringInHumanBeingReadingStyle(new DateTime(dateLogFileGenerated));
        } else {
            return (String) getConvertedTime((dateCheckTheDuration.getTime() - dateLogFileGenerated.getTime()) / 1000);
        }
    }

    static interface ConvertedTime {
        String getConvertedTime(long duration);
    }

    static class LessThanAMinute implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return TimeConverter.LESS_THAN_A_MINUTE;
        }
    }

    static class AboutOneMinute implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return TimeConverter.FROM_1_MINUTE;
        }
    }

    static class From2To44Minutes implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return StringUtils.replace(TimeConverter.FROM_2_TO_44_MINUTES, "$time", String
                    .valueOf((duration + 30) / 60));
        }
    }

    static class AboutOneHour implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return TimeConverter.ABOUT_1_HOUR;
        }
    }

    static class About2To24Hours implements ConvertedTime {
        public String getConvertedTime(long duration) {
            long hours = (duration + 30 * 60 + 30) / TimeConverter.HOUR_IN_SECONDS;
            return StringUtils.replace(TimeConverter.ABOUT_2_TO_24_HOURS, "$time", String.valueOf(hours >= 23
                    ? 23 : hours));

        }
    }

    static class AboutOneDay implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return TimeConverter.ABOUT_1_DAY;
        }
    }

    static class From2To29Days implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return StringUtils.replace(TimeConverter.FROM_2_TO_29_DAYS, "$time", String
                    .valueOf((duration + 30) / TimeConverter.DAY_IN_SECONDS));
        }
    }

    static class AboutOneMonth implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return TimeConverter.ABOUT_1_MONTH;
        }
    }

    static class From2To12Month implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return StringUtils.replace(TimeConverter.FROM_2_TO_12_MONTHS, "$time", String
                    .valueOf((duration + 30) / TimeConverter.MONTH_IN_SECONDS));
        }
    }

    static class AboutOneYear implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return TimeConverter.ABOUT_1_YEAR;
        }

    }

    static class OverTwoYears implements ConvertedTime {
        public String getConvertedTime(long duration) {
            return StringUtils.replace(TimeConverter.OVER_2_YEARS, "$time", String.valueOf((duration + 30)
                    / TimeConverter.YEAR_IN_SECONDS));
        }
    }
}

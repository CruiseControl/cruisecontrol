package net.sourceforge.cruisecontrol.dashboard.utils;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public class TimeConverterTest extends TestCase {
    private TimeConverter timeConverter;

    protected void setUp() throws Exception {
        this.timeConverter = new TimeConverter();
    }

    public void testShouldReturn() {
        DateTime now = new DateTime();
        DateTime yesterday = now.minusDays(1);
        assertEquals(CCDateFormatter.getDateStringInHumanBeingReadingStyle(now), timeConverter
                .getConvertedTime(now.toDate(), yesterday.toDate()));
    }

    public void testShouldReportLessThanOneMinutesFor0To29Seconds() {
        assertEquals(TimeConverter.LESS_THAN_A_MINUTE, timeConverter.getConvertedTime(29));
    }

    public void testShouldReportOneMinuteFor30Seconds() {
        assertEquals(TimeConverter.FROM_1_MINUTE, timeConverter.getConvertedTime(30));
    }

    public void testShouldReportOneMinuteFor89Seconds() {
        assertEquals(TimeConverter.FROM_1_MINUTE, timeConverter.getConvertedTime(89));
    }

    public void testShouldReport2To44MinutesFor90Seconds() {
        assertEquals(StringUtils.replace(TimeConverter.FROM_2_TO_44_MINUTES, "$time", "2"), timeConverter
                .getConvertedTime(1 * 60 + 30));
    }

    public void testShouldReport1DayFor45Mintues() {
        assertEquals(TimeConverter.ABOUT_1_HOUR, timeConverter.getConvertedTime(45 * 60));
    }

    public void testShouldReport44MinutesFor44Minutes29Seconds() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.FROM_2_TO_44_MINUTES, "$time", "44"), timeConverter
                .getConvertedTime(44 * 60 + 29));
    }

    public void testShouldReportAbout1HourFor44Minutes30Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_HOUR, timeConverter.getConvertedTime(44 * 60 + 30));
    }

    public void testShouldReportAbout1HourFor89Minutes29Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_HOUR, timeConverter.getConvertedTime(89 * 60 + 29));
    }

    public void testShouldReportAbout2HoursHourFor89Minutes30Seconds() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.ABOUT_2_TO_24_HOURS, "$time", "2"), timeConverter
                .getConvertedTime(1 * TimeConverter.HOUR_IN_SECONDS + 29 * 60 + 30));
    }

    public void testShouldReport23HoursFor23Hours59Minutes29Seconds() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.ABOUT_2_TO_24_HOURS, "$time", "23"), timeConverter
                .getConvertedTime(24 * TimeConverter.HOUR_IN_SECONDS - 31));
    }

    public void testShouldReportAbout1DayFor23Hours59Minutes30Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_DAY, timeConverter.getConvertedTime(23 * 60 * 60 + 59 * 60 + 30));
    }

    public void testShouldReportAbout1DayFor47Hours59Minutes29Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_DAY, timeConverter.getConvertedTime(47 * 60 * 60 + 59 * 60 + 29));
    }

    public void testShouldReport2DaysFor47Hours59Minutes29Seconds() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.FROM_2_TO_29_DAYS, "$time", "2"), timeConverter
                .getConvertedTime(2 * TimeConverter.DAY_IN_SECONDS - 30));
    }

    public void testShouldReport29DaysFor29Days23Hours59Minutes29Seconds() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.FROM_2_TO_29_DAYS, "$time", "29"), timeConverter
                .getConvertedTime(30 * TimeConverter.DAY_IN_SECONDS - 31));
    }

    public void testShouldReportAbout1MonthFor29Days23Hours59Minutes30Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_MONTH, timeConverter.getConvertedTime(29
                * TimeConverter.DAY_IN_SECONDS + 23 * 60 * 60 + 59 * 60 + 30));
    }

    public void testShouldReportAbout1MonthFor59Days23Hours59Minutes29Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_MONTH, timeConverter.getConvertedTime(59
                * TimeConverter.DAY_IN_SECONDS + 23 * 60 * 60 + 59 * 60 + 29));
    }

    public void testShouldReport2MonthsFor59Days23Hours59Minutes30Seconds() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.FROM_2_TO_12_MONTHS, "$time", "2"), timeConverter
                .getConvertedTime(60 * TimeConverter.DAY_IN_SECONDS - 30));
    }

    public void testShouldReport12MonthsFor59Days23Hours59Minutes30Seconds() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.FROM_2_TO_12_MONTHS, "$time", "12"), timeConverter
                .getConvertedTime(365 * TimeConverter.DAY_IN_SECONDS - 31));
    }

    public void testShouldReportAbout1YearFor1YearMinus30Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_YEAR, timeConverter
                .getConvertedTime(365 * TimeConverter.DAY_IN_SECONDS - 30));
    }

    public void testShouldReportAbout1YearFor2YearsMinus31Seconds() throws Exception {
        assertEquals(TimeConverter.ABOUT_1_YEAR, timeConverter
                .getConvertedTime(2 * 365 * TimeConverter.DAY_IN_SECONDS - 31));
    }

    public void testShouldReturnTimeUnitAsYearsWhenDurationIsLargerThan2Years() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.OVER_2_YEARS, "$time", "2"), timeConverter
                .getConvertedTime(2 * 365 * TimeConverter.DAY_IN_SECONDS - 30));
    }

    public void testShouldReturnTimeUnitAsYearsWhenDurationIsLargerThan3Years() throws Exception {
        assertEquals(StringUtils.replace(TimeConverter.OVER_2_YEARS, "$time", "3"), timeConverter
                .getConvertedTime(3 * 365 * TimeConverter.DAY_IN_SECONDS + 2 * TimeConverter.DAY_IN_SECONDS));
    }

}

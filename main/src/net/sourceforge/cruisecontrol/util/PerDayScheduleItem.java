package net.sourceforge.cruisecontrol.util;

import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.util.Locale;

import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;

import org.apache.log4j.Logger;

public class PerDayScheduleItem implements Serializable {

    private static final long serialVersionUID = 9018513455979334615L;
    private static final Logger LOG = Logger.getLogger(PerDayScheduleItem.class);

    protected static final int INVALID_NAME_OF_DAY = -2;

    public static final int NOT_SET = -1;

    private int day = NOT_SET;

    public int getDay() {
        return day;
    }

    @Description("Valid values are (case-insensitive) the English names for the days of the "
            + "week (Sunday, Monday, Tuesday, etc). Does not support multiple days except for "
            + "the default of every day.")
    @Optional
    public void setDay(String dayString) {
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.ENGLISH);
        String[] weekdays = symbols.getWeekdays();
        for (int i = 1; i < weekdays.length; i++) {
            if (dayString.equalsIgnoreCase(weekdays[i])) {
                day = i;
                return;
            }
        }
        day = INVALID_NAME_OF_DAY;
        LOG.warn("invalid value for day attribute \"" + dayString + "\"; must be English name for day of week");
    }

}

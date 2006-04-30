package net.sourceforge.cruisecontrol.util;

import java.util.Calendar;

import junit.framework.TestCase;

public class PerDayScheduleItemTest extends TestCase {

    public void testSetDayShouldMapEnglishNamesToCalendarIntValues() {
        PerDayScheduleItem item = new PerDayScheduleItem();
        assertEquals(PerDayScheduleItem.NOT_SET, item.getDay());
        item.setDay("Sunday");
        assertEquals(Calendar.SUNDAY, item.getDay());
        item.setDay("saturday");
        assertEquals(Calendar.SATURDAY, item.getDay());
        item.setDay("foo");
        assertEquals(PerDayScheduleItem.INVALID_NAME_OF_DAY, item.getDay());
    }
    
}

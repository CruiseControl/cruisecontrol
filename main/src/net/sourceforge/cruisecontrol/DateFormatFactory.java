package net.sourceforge.cruisecontrol;

public class DateFormatFactory {

    private static String _format = "MM/dd/yyyy HH:mm:ss";

    public static String getFormat() {
        return _format;
    }

    public static void setFormat(String format) {
        _format = format;
    }
}

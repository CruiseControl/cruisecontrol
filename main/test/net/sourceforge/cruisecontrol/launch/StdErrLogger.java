package net.sourceforge.cruisecontrol.launch;

final class StdErrLogger implements LogInterface {
    @Override
    public void error(Object message) {
        System.err.println("ERROR: " + message);
    }
    @Override
    public void warn(Object message) {
        System.err.println("WARN: " + message);
    }
    @Override
    public void info(Object message) {
        System.err.println("INFO: " + message);
    }
    @Override
    public void flush(LogInterface log) throws LaunchException {
        throw new IllegalStateException("Flush not supported");

    }
}
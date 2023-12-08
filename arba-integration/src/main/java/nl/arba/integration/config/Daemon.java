package nl.arba.integration.config;

public class Daemon {
    private Step[] steps;
    private long interval;

    public void setSteps(Step[] steps) {
        this.steps = steps;
    }

    public Step[] getSteps() {
        return steps;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getInterval() {
        return interval;
    }
}

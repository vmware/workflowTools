import java.util.Date;

public class PlaceDuration {
    public long startTimestampMs;

    public long endTimestampMs;

    public Date startDate() {
        return new Date(startTimestampMs);
    }

    public Date endDate() {
        return new Date(endTimestampMs);
    }
}

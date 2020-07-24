import java.util.List;
import java.util.stream.Collectors;

public class TimelineObjects {
    public List<TimelineObject> timelineObjects;

    public List<PlaceVisit> placesVisited() {
        return timelineObjects.stream()
                .filter(timelineObject -> timelineObject.placeVisit != null)
                .map(timelineObject -> timelineObject.placeVisit)
                .filter(placeVisit -> placeVisit.location != null)
                .filter(placeVisit -> placeVisit.location.address != null).collect(Collectors.toList());
    }
}

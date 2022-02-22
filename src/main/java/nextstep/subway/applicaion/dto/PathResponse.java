package nextstep.subway.applicaion.dto;

import nextstep.subway.domain.Path;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class PathResponse {
    private List<StationResponse> stations;
    private int distance;
    private int duration;
    private BigDecimal fare;

    public PathResponse(List<StationResponse> stations, int distance, int duration, BigDecimal fare) {
        this.stations = stations;
        this.distance = distance;
        this.duration = duration;
        this.fare = fare;
    }

    public static PathResponse of(Path path) {
        return of(path, path);
    }

    public static PathResponse of(Path path, Path shortestPath) {
        List<StationResponse> stations = path.getStations().stream()
                .map(StationResponse::of)
                .collect(Collectors.toList());
        int distance = path.extractDistance();
        int duration = path.extractDuration();
        BigDecimal fare = shortestPath.extractFare();

        return new PathResponse(stations, distance, duration, fare);
    }

    public List<StationResponse> getStations() {
        return stations;
    }

    public int getDistance() {
        return distance;
    }

    public int getDuration() {
        return duration;
    }

    public BigDecimal getFare() {
        return fare;
    }
}

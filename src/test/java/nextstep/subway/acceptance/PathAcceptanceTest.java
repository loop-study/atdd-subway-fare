package nextstep.subway.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static nextstep.subway.acceptance.LineSteps.지하철_노선에_지하철_구간_생성_요청;
import static nextstep.subway.acceptance.MemberSteps.*;
import static nextstep.subway.acceptance.StationSteps.지하철역_생성_요청;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 경로 검색")
class PathAcceptanceTest extends AcceptanceTest {
    private static final String DISTANCE_TYPE = "DISTANCE";
    private static final String DURATION_TYPE = "DURATION";
    private static final String EMAIL = "email@email.com";
    private static final String PASSWORD = "password";
    public static final String USERNAME_FIELD = "username";
    public static final String PASSWORD_FIELD = "password";

    private Long 교대역;
    private Long 강남역;
    private Long 양재역;
    private Long 남부터미널역;
    private Long 잠실역;
    private Long 이호선;
    private Long 신분당선;
    private Long 삼호선;

    /**        (10Km, 5분)               (100km 1분)
     * 교대역    --- *2호선* ---   강남역 --- *2호선* --- 잠실역
     * |                        |                      |
     * (2km, 1분)            (10km, 3분)          (1km 100분)
     * *3호선*                   *신분당선*     *임의로 연결한3호선*
     * |                        |                |
     * 남부터미널역  --- *3호선* --- 양재  ---------|
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        교대역 = 지하철역_생성_요청("교대역").jsonPath().getLong("id");
        강남역 = 지하철역_생성_요청("강남역").jsonPath().getLong("id");
        양재역 = 지하철역_생성_요청("양재역").jsonPath().getLong("id");
        남부터미널역 = 지하철역_생성_요청("남부터미널역").jsonPath().getLong("id");
        잠실역 = 지하철역_생성_요청("잠실역").jsonPath().getLong("id");

        이호선 = 지하철_노선_생성_요청("2호선", "green", 교대역, 강남역, 10, 5);
        신분당선 = 지하철_노선_생성_요청("신분당선", "red", 강남역, 양재역, 10, 3);
        삼호선 = 지하철_노선_생성_요청("3호선", "orange", 교대역, 남부터미널역, 2, 1);

        지하철_노선에_지하철_구간_생성_요청(이호선, createSectionCreateParams(강남역, 잠실역, 100, 1));
        지하철_노선에_지하철_구간_생성_요청(삼호선, createSectionCreateParams(남부터미널역, 양재역, 3, 2));
        지하철_노선에_지하철_구간_생성_요청(삼호선, createSectionCreateParams(양재역, 잠실역, 1, 100));
    }

    /**
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청
     * Then 최단 거리 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금도 함께 응답함
     */
    @DisplayName("두 역의 최단 거리 경로를 조회한다.")
    @Test
    void findPathByDistance() {
        // when
        ExtractableResponse<Response> response = 두_역의_최단_거리_경로_요청(교대역, 양재역);

        // then
        Assertions.assertAll(
                () -> assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(교대역, 남부터미널역, 양재역),
                () -> assertThat(response.jsonPath().getInt("distance")).isEqualTo(5),
                () -> assertThat(response.jsonPath().getInt("duration")).isEqualTo(3),
                () -> assertThat(response.jsonPath().getInt("fare")).isEqualTo(1250)
        );
    }

    /**
     * When 출발역(남부터미널역)에서 도착역(강남역)까지의 최단 시간 경로 조회를 요청
     * Then 최단 시간 경로를 응답
     * And 총 거리(13km)와 소요 시간(5)을 함께 응답함
     * And 지하철 이용 요금(최단 거리는 남부터미널역 2km 교대역 10km 강남역으로 12km 기준으로 계산됨) 도 함께 응답함
     */
    @DisplayName("두 역의 최단 시간 경로를 조회한다.")
    @Test
    void findPathByDuration() {
        // when
        ExtractableResponse<Response> response = 두_역의_최단_시간_경로_요청(남부터미널역, 강남역);

        // then
        Assertions.assertAll(
                ()-> assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(남부터미널역, 양재역, 강남역),
                ()-> assertThat(response.jsonPath().getInt("distance")).isEqualTo(13),
                ()-> assertThat(response.jsonPath().getInt("duration")).isEqualTo(5),
                ()-> assertThat(response.jsonPath().getInt("fare")).isEqualTo(1350)
        );
    }

    /**
     * When 출발역(잠실역)에서 도착역(양재역)까지의 최단 시간 경로 조회를 요청
     * Then 최단 시간 경로를 응답
     * And 총 거리(110km)와 소요 시간(4)을 함께 응답함
     * And 지하철 이용 요금(최단 거리는 잠실역과 -- 1km --> 양재역으로 기본요금만 계산됨)도 함께 응답함
     */
    @DisplayName("두 역의 최단 시간 경로를 조회해도 요금은 실제 최단 거리로 계산한다.")
    @Test
    void findPathByDurationBut() {
        // when
        ExtractableResponse<Response> response = 두_역의_최단_시간_경로_요청(잠실역, 양재역);

        // then
        Assertions.assertAll(
                ()-> assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(잠실역, 강남역, 양재역),
                ()-> assertThat(response.jsonPath().getInt("distance")).isEqualTo(110),
                ()-> assertThat(response.jsonPath().getInt("duration")).isEqualTo(4),
                ()-> assertThat(response.jsonPath().getInt("fare")).isEqualTo(1250)
        );
    }

    /**
     * Given 어린이 회원 생성을 요청
     * Given 생성된 어린이 로그인
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청
     * Then 최단 거리 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 기본 지하철 이용 요금(1250)은 어린이 할인(공제액 350원을 제외한 900원의 50퍼 -> 800원)을 받은 상태로 응답함
     */
    @DisplayName("최단 거리 경로의 어린이 요금제를 확인한다.")
    @Test
    void childFare() {
        // Given
        회원_생성_요청(EMAIL, PASSWORD, 10);
        String accessToken = 로그인_되어_있음(EMAIL, PASSWORD);

        // when
        ExtractableResponse<Response> response = 로그인_두_역의_최단_거리_경로_요청(교대역, 양재역, accessToken);

        // then
        Assertions.assertAll(
                () -> assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(교대역, 남부터미널역, 양재역),
                () -> assertThat(response.jsonPath().getInt("distance")).isEqualTo(5),
                () -> assertThat(response.jsonPath().getInt("duration")).isEqualTo(3),
                () -> assertThat(response.jsonPath().getInt("fare")).isEqualTo(800)
        );
    }

    private ExtractableResponse<Response> 두_역의_최단_거리_경로_요청(Long source, Long target) {
        return 두_역의_최단_경로를_요청(source, target, DISTANCE_TYPE, null);
    }

    private ExtractableResponse<Response> 두_역의_최단_시간_경로_요청(Long source, Long target) {
        return 두_역의_최단_경로를_요청(source, target, DURATION_TYPE, null);
    }

    private ExtractableResponse<Response> 로그인_두_역의_최단_거리_경로_요청(Long source, Long target, String accessToken) {
        return 두_역의_최단_경로를_요청(source, target, DISTANCE_TYPE, accessToken);
    }

    private ExtractableResponse<Response> 로그인_두_역의_최단_시간_경로_요청(Long source, Long target, String accessToken) {
        return 두_역의_최단_경로를_요청(source, target, DURATION_TYPE, accessToken);
    }

    private ExtractableResponse<Response> 두_역의_최단_경로를_요청(Long source, Long target, String type, String accessToken) {
        return RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("source", source)
                .queryParam("target", target)
                .queryParam("type", type)
                .when().get("/paths")
                .then().log().all().extract();
    }

    private Long 지하철_노선_생성_요청(String name, String color, Long upStation, Long downStation, int distance, int duration) {
        Map<String, String> lineCreateParams;
        lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", name);
        lineCreateParams.put("color", color);
        lineCreateParams.put("upStationId", upStation + "");
        lineCreateParams.put("downStationId", downStation + "");
        lineCreateParams.put("distance", distance + "");
        lineCreateParams.put("duration", duration + "");

        return LineSteps.지하철_노선_생성_요청(lineCreateParams).jsonPath().getLong("id");
    }

    private Map<String, String> createSectionCreateParams(Long upStationId, Long downStationId, int distance, int duration) {
        Map<String, String> params = new HashMap<>();
        params.put("upStationId", upStationId + "");
        params.put("downStationId", downStationId + "");
        params.put("distance", distance + "");
        params.put("duration", duration + "");
        return params;
    }
}

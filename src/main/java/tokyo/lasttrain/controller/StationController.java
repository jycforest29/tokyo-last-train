package tokyo.lasttrain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tokyo.lasttrain.dto.StationSearchResponse;
import tokyo.lasttrain.dto.StationSearchResponse.StationInfo;
import tokyo.lasttrain.service.StationSearchService;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationSearchService stationSearchService;

    public StationController(StationSearchService stationSearchService) {
        this.stationSearchService = stationSearchService;
    }

    /**
     * 역 이름 검색 (출발역/도착역 공통)
     * 영어, 일본어 모두 검색 가능
     *
     * GET /api/v1/stations/search?query=shibuya
     * GET /api/v1/stations/search?query=渋谷
     */
    @GetMapping("/search")
    public StationSearchResponse search(@RequestParam String query) {
        return stationSearchService.search(query);
    }

    /**
     * 가장 가까운 역을 반환.
     *
     * 개인정보 보호 원칙:
     *   - lat/lng는 SLF4J 로거에 절대 출력하지 않는다.
     *   - 응답 후 즉시 stack 밖으로 소멸하며, 어떠한 저장소(메모리 캐시 포함)에도 보관하지 않는다.
     *   - Nginx access log는 별도로 query string 마스킹 설정이 필요하다 (scripts/ec2-setup.sh 참조).
     *
     * GET /api/v1/stations/nearest?lat=35.658&lng=139.7016
     */
    @GetMapping("/nearest")
    public ResponseEntity<StationInfo> nearest(
            @RequestParam("lat") double latitude,
            @RequestParam("lng") double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("lat must be in [-90,90] and lng in [-180,180]");
        }
        StationInfo result = stationSearchService.findNearestStation(latitude, longitude);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
}
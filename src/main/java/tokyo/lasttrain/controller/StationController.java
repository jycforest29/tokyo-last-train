package tokyo.lasttrain.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tokyo.lasttrain.dto.StationSearchResponse;
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
}
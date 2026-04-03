package tokyo.lasttrain.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tokyo.lasttrain.dto.LastTrainResponse;
import tokyo.lasttrain.service.LastTrainService;

@RestController
@RequestMapping("/api/v1/last-train")
public class LastTrainController {

    private final LastTrainService lastTrainService;

    public LastTrainController(LastTrainService lastTrainService) {
        this.lastTrainService = lastTrainService;
    }

    /**
     * 현재 시간 기준 막차 경로 조회
     *
     * GET /api/v1/last-train?from=odpt.Station:TokyoMetro.Ginza.Shibuya&to=odpt.Station:JR-East.Yamanote.Tokyo
     */
    @GetMapping
    public LastTrainResponse findLastTrain(
            @RequestParam String from,
            @RequestParam String to
    ) {
        return lastTrainService.findLastTrain(from, to);
    }
}
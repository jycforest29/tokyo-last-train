package tokyo.lasttrain.service.impl;

import org.springframework.stereotype.Service;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.dto.StationSearchResponse;
import tokyo.lasttrain.dto.StationSearchResponse.StationInfo;
import tokyo.lasttrain.model.OdptRailway;
import tokyo.lasttrain.model.OdptStation;
import tokyo.lasttrain.service.StationSearchService;

import java.util.List;
import java.util.Map;

@Service
public class StationSearchServiceImpl implements StationSearchService {

    private final TransitDataCache cache;

    public StationSearchServiceImpl(TransitDataCache cache) {
        this.cache = cache;
    }

    @Override
    public StationSearchResponse search(String query) {
        List<String> stationIds = cache.searchStations(query);

        List<StationInfo> stations = stationIds.stream()
                .map(cache::getStation)
                .filter(s -> s != null)
                .map(this::toStationInfo)
                .limit(20)
                .toList();

        return new StationSearchResponse(stations);
    }

    /**
     * 입력 좌표에서 가장 가까운 역을 반환한다.
     * 좌표는 메모리에서만 사용되며 로깅하지 않는다.
     */
    @Override
    public StationInfo findNearestStation(double latitude, double longitude) {
        OdptStation nearest = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (OdptStation s : cache.getAllStations()) {
            Double sLat = s.latitude();
            Double sLng = s.longitude();
            if (sLat == null || sLng == null) continue;

            // 가까운 역 비교만 하므로 정확한 haversine 대신 위경도 차이 제곱합으로 충분.
            // (도쿄권은 위도가 거의 일정하므로 평면 근사 정확도도 적절.)
            double dLat = sLat - latitude;
            double dLng = sLng - longitude;
            double distSq = dLat * dLat + dLng * dLng;

            if (distSq < bestDistanceSq) {
                bestDistanceSq = distSq;
                nearest = s;
            }
        }

        return nearest != null ? toStationInfo(nearest) : null;
    }

    private StationInfo toStationInfo(OdptStation station) {
        String nameJa = station.title();
        String nameEn = getLocalizedName(station.stationTitle(), "en");
        String nameKo = cache.getStationNameKo(station.id());

        String railwayNameJa = null;
        String railwayNameEn = null;
        String railwayNameKo = null;
        if (station.railway() != null) {
            OdptRailway railway = cache.getRailway(station.railway());
            if (railway != null) {
                railwayNameJa = railway.title();
                railwayNameEn = getLocalizedName(railway.railwayTitle(), "en");
                railwayNameKo = cache.getRailwayNameKo(station.railway());
            }
        }

        return new StationInfo(
                station.id(),
                nameJa,
                nameEn,
                nameKo,
                station.railway(),
                railwayNameJa,
                railwayNameEn,
                railwayNameKo,
                station.operator(),
                station.latitude(),
                station.longitude()
        );
    }

    private String getLocalizedName(Map<String, String> titleMap, String lang) {
        if (titleMap == null) return null;
        return titleMap.get(lang);
    }
}
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

    private StationInfo toStationInfo(OdptStation station) {
        String nameJa = station.title();
        String nameEn = getLocalizedName(station.stationTitle(), "en");

        String railwayNameJa = null;
        String railwayNameEn = null;
        if (station.railway() != null) {
            OdptRailway railway = cache.getRailway(station.railway());
            if (railway != null) {
                railwayNameJa = railway.title();
                railwayNameEn = getLocalizedName(railway.railwayTitle(), "en");
            }
        }

        return new StationInfo(
                station.id(),
                nameJa,
                nameEn,
                station.railway(),
                railwayNameJa,
                railwayNameEn,
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
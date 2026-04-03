package tokyo.lasttrain.service;

import tokyo.lasttrain.dto.StationSearchResponse;

public interface StationSearchService {

    StationSearchResponse search(String query);
}
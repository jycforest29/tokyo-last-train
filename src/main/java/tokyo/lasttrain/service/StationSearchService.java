package tokyo.lasttrain.service;

import tokyo.lasttrain.dto.StationSearchResponse;
import tokyo.lasttrain.dto.StationSearchResponse.StationInfo;

public interface StationSearchService {

    StationSearchResponse search(String query);

    /**
     * 입력 좌표에서 가장 가까운 역을 반환한다.
     * 호출 측은 좌표를 절대 로깅하지 않아야 한다 (개인정보 보호).
     */
    StationInfo findNearestStation(double latitude, double longitude);
}
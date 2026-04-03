package tokyo.lasttrain.service;

import tokyo.lasttrain.dto.LastTrainResponse;

public interface LastTrainService {

    LastTrainResponse findLastTrain(String fromStationId, String toStationId);
}
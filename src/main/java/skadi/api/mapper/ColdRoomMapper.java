package skadi.api.mapper;

import skadi.api.dto.ColdRoomRequest;
import skadi.api.dto.ColdRoomResponse;
import skadi.api.model.ColdRoom;
import skadi.api.model.DistributionCenter;
import skadi.api.model.Thermometer;

public final class ColdRoomMapper {

    private ColdRoomMapper() {
    }

    public static ColdRoom fromRequest(
            ColdRoomRequest request,
            DistributionCenter distributionCenter,
            Thermometer thermometer
    ) {
        return new ColdRoom(
                null,
                request.model().trim(),
                request.location().trim(),
                request.minimumTemperature(),
                request.maximumTemperature(),
                distributionCenter,
                thermometer
        );
    }

    public static void update(
            ColdRoom coldRoom,
            ColdRoomRequest request,
            DistributionCenter distributionCenter,
            Thermometer thermometer
    ) {
        coldRoom.setModel(request.model().trim());
        coldRoom.setLocation(request.location().trim());
        coldRoom.setMinimumTemperature(request.minimumTemperature());
        coldRoom.setMaximumTemperature(request.maximumTemperature());
        coldRoom.setDistributionCenter(distributionCenter);
        coldRoom.setThermometer(thermometer);
    }

    public static ColdRoomResponse toResponse(ColdRoom coldRoom) {
        return new ColdRoomResponse(
                coldRoom.getId(),
                coldRoom.getModel(),
                coldRoom.getLocation(),
                coldRoom.getMinimumTemperature(),
                coldRoom.getMaximumTemperature(),
                coldRoom.getDistributionCenter().getId(),
                coldRoom.getDistributionCenter().getName(),
                coldRoom.getThermometer().getId(),
                coldRoom.getThermometer().getModel()
        );
    }
}

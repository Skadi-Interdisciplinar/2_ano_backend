package skadi.api.mapper;

import skadi.api.dto.ThermometerRequest;
import skadi.api.dto.ThermometerResponse;
import skadi.api.model.Thermometer;

public final class ThermometerMapper {

    private ThermometerMapper() {
    }

    public static Thermometer fromRequest(ThermometerRequest request) {
        return new Thermometer(null, request.model().trim());
    }

    public static void update(Thermometer thermometer, ThermometerRequest request) {
        thermometer.setModel(request.model().trim());
    }

    public static ThermometerResponse toResponse(Thermometer thermometer) {
        return new ThermometerResponse(thermometer.getId(), thermometer.getModel());
    }
}

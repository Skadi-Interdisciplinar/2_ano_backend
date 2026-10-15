package skadi.api.mapper;

import skadi.api.dto.StateResponse;
import skadi.api.model.State;

public final class StateMapper {

    private StateMapper() {
    }

    public static StateResponse toResponse(State state) {
        return new StateResponse(state.getId(), state.getAbbreviation().trim());
    }
}

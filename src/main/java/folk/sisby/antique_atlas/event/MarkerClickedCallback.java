package folk.sisby.antique_atlas.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import folk.sisby.antique_atlas.marker.Marker;
import net.minecraft.entity.player.PlayerEntity;

@FunctionalInterface
public interface MarkerClickedCallback {
    Event<MarkerClickedCallback> EVENT = EventFactory.createEventResult();

    EventResult onClicked(PlayerEntity player, Marker marker, int mouseState);
}

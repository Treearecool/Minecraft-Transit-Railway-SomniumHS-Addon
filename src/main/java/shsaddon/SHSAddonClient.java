package shsaddon;

import mtr.MTR;
import mtr.config.Config;
import mtr.packet.IPacket;
import shsaddon.render.RenderPIDSSHS;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.client.MinecraftClient;

public class SHSAddonClient implements ClientModInitializer, IPacket {

	@Override
	public void onInitializeClient() {

		BlockEntityRendererRegistry.INSTANCE.register(MTR.PIDS_2_TILE_ENTITY, dispatcher -> new RenderPIDSSHS<>(dispatcher, false));

		Config.refreshProperties();

		ClientEntityEvents.ENTITY_LOAD.register((entity, clientWorld) -> {
			if (entity == MinecraftClient.getInstance().player) {
				Config.refreshProperties();
			}
		});
	}
}

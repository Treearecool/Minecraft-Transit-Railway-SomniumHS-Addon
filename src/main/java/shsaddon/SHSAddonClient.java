package shsaddon;

import mtr.config.Config;
import mtr.packet.IPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import shsaddon.render.RenderPIDSSHS;

public class SHSAddonClient implements ClientModInitializer, IPacket {

	@Override
	public void onInitializeClient() {

		BlockEntityRendererRegistry.INSTANCE.register(SHSAddon.PIDS_SHS_TILE_ENTITY, dispatcher -> new RenderPIDSSHS<>(dispatcher, true));
//		BlockEntityRendererRegistry.INSTANCE.register(SHSAddon.PIDS_SHS_ROUTE_TILE_ENTITY, RenderPIDSSHSRoute::new);

		Config.refreshProperties();

		ClientEntityEvents.ENTITY_LOAD.register((entity, clientWorld) -> {
			if (entity == MinecraftClient.getInstance().player) {
				Config.refreshProperties();
			}
		});
	}
}

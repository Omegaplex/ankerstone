package io.github.omegaplex.ankerstone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ankerstone implements ModInitializer {
	public static final String MOD_ID = "ankerstone";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ServerPlayConnectionEvents.JOIN
				.register((handler, sender, server) -> ModAdvancements.onPlayerJoin(handler.player));
		// Rebuild live chunk tickets from saved data whenever a server dimension loads.
		ServerLevelEvents.LOAD.register((server, level) -> ChunkTickets.restore(level));
		// Discard runtime ownership on unload; saved owners remain for restoration.
		ServerLevelEvents.UNLOAD.register((server, level) -> ChunkTickets.clear(level));

		LOGGER.info("Ankerstone initialized!");
	}
}

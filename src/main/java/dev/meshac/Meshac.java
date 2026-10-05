package dev.meshac;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Meshac implements ModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("meshac");

	@Override
	public void onInitialize() {
		LOG.info("meshac loaded");
	}
}

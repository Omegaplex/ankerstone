package io.github.omegaplex.ankerstone;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClientResources implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var container = FabricLoader.getInstance()
                .getModContainer(Ankerstone.MOD_ID)
                .orElseThrow();

        boolean registered = ResourceLoader.registerBuiltinPack(
                Identifier.fromNamespaceAndPath(Ankerstone.MOD_ID, "pom"),
                container,
                Component.literal("Ankerstone POM Compatibility"),
                PackActivationType.NORMAL);

        if (!registered) {
            Ankerstone.LOGGER.warn("Could not register the POM compatibility pack");
        }
    }
}
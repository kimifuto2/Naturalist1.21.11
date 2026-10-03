package com.crispytwig.naturalist.fabric.client;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.NaturalistClient;
import com.crispytwig.naturalist.NaturalistClientConfig;
import com.crispytwig.naturalist.fabric.config.FabricNaturalistClientConfig;
import com.crispytwig.naturalist.client.particle.CaptureNetSwingParticle;
import com.crispytwig.naturalist.registry.NaturalistParticleTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.resources.Identifier;

public class NaturalistFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricNaturalistClientConfig.load();
        NaturalistClientConfig.setGlowGoopTooltip(FabricNaturalistClientConfig::isGlowGoopTooltipEnabled);

        NaturalistClient.registerLayerDefinitions((location, definition) ->
                EntityModelLayerRegistry.registerModelLayer(location, definition::get));
        NaturalistClient.registerRenderers(EntityRendererRegistry::register);
        NaturalistClient.registerBlockEntityRenderers(BlockEntityRenderers::register);

        NaturalistClient.registerMenuScreens(MenuScreens::register);

        NaturalistClient.registerConditionalItemModelProperties(ConditionalItemModelProperties.ID_MAPPER::put);

        ParticleFactoryRegistry.getInstance().register(NaturalistParticleTypes.CAPTURE_NET_SWING.get(), CaptureNetSwingParticle.Provider::new);

        registerBuiltinPacks();
    }

    /**
     * Fabric does not auto-discover resource packs that are bundled inside a mod jar: without an
     * explicit registration the "custom_spawn_eggs" overlay ships as inert payload that never shows
     * up in the resource pack screen. ResourceLoader.registerBuiltinPack maps the identifier's path
     * directly onto the jar folder {@code resourcepacks/<path>}, i.e. resourcepacks/custom_spawn_eggs.
     *
     * <p>PackActivationType.NORMAL is deliberate: the pack is an alternative cosmetic restyle of the
     * mod's own spawn egg item models (see its pack.mcmeta description), so it is listed and left for
     * the player to opt into. DEFAULT_ENABLED would silently override every player's spawn egg look and
     * would fight any other pack touching the same model ids, and ALWAYS_ENABLED would remove the
     * ability to turn a purely cosmetic change back off.
     */
    private static void registerBuiltinPacks() {
        Identifier packId = Identifier.fromNamespaceAndPath(Naturalist.MOD_ID, "custom_spawn_eggs");
        // Defensive: never advertise a pack whose payload was stripped from the jar. Registering a pack
        // with a missing folder does not throw, it would just add an empty/broken entry to the pack list.
        if (NaturalistFabricClient.class.getResource("/resourcepacks/custom_spawn_eggs/pack.mcmeta") == null) {
            Naturalist.LOGGER.warn("Bundled resource pack '{}' is not present in the jar; skipping registration", packId);
            return;
        }
        FabricLoader.getInstance().getModContainer(Naturalist.MOD_ID).ifPresentOrElse(
                container -> ResourceLoader.registerBuiltinPack(packId, container, PackActivationType.NORMAL),
                () -> Naturalist.LOGGER.warn("Could not resolve the '{}' mod container; bundled resource pack not registered", Naturalist.MOD_ID));
    }
}

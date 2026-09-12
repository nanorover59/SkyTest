package nano.skytest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class SkytestClient implements ClientModInitializer {
    private static final SimpleQuadTexture simpleQuadTexture = new SimpleQuadTexture();

    public static Identifier modID(String id) {
        return Identifier.fromNamespaceAndPath("skytest", id);
    }

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, modID("before_chat"), SkytestClient::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        simpleQuadTexture.prepareTexture();
        GpuTextureView textureView = simpleQuadTexture.getTexture();
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        graphics.blit(textureView, sampler, 0, 0, 64, 64, 0.0f, 1.0f, 1.0f, 0.0f);
    }
}
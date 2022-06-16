package net.mine_diver.qpf.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.mine_diver.qpf.QuarkPickarangFabric;
import net.mine_diver.qpf.content.tools.client.render.entity.PickarangRenderer;

public class QuarkPickarangFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(QuarkPickarangFabric.PICKARANG_TYPE, PickarangRenderer::new);
    }
}

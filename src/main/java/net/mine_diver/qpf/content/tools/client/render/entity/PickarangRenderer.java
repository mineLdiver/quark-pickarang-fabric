package net.mine_diver.qpf.content.tools.client.render.entity;

import net.mine_diver.qpf.content.tools.entity.Pickarang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;

import javax.annotation.Nonnull;

public class PickarangRenderer extends EntityRenderer<Pickarang> {

	public PickarangRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(Pickarang entity, float yaw, float partialTicks, MatrixStack matrix, @Nonnull VertexConsumerProvider buffer, int light) {
		matrix.push();
		matrix.translate(0, 0.2, 0);
		matrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90F));

		MinecraftClient mc = MinecraftClient.getInstance();
		float time = entity.age + (mc.isPaused() ? 0 : partialTicks);
		matrix.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(time * 20F));

		mc.getItemRenderer().renderItem(entity.getStack(), ModelTransformation.Mode.FIXED, light, OverlayTexture.DEFAULT_UV, matrix, buffer, 0);

		matrix.pop();
	}

	@Nonnull
	@Override
	public Identifier getTexture(@Nonnull Pickarang entity) {
		//noinspection ConstantConditions
		return null;
	}
}

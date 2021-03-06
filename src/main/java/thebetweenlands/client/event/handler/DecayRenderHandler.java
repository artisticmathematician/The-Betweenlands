package thebetweenlands.client.event.handler;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import thebetweenlands.api.capability.IDecayCapability;
import thebetweenlands.common.lib.ModInfo;
import thebetweenlands.common.registries.CapabilityRegistry;

public class DecayRenderHandler {
	public static final ResourceLocation PLAYER_DECAY_TEXTURE = new ResourceLocation(ModInfo.ID, "textures/entity/player_decay.png");

	private static Field fieldLayerRenderers = ReflectionHelper.findField(RenderLivingBase.class, "layerRenderers", "field_177097_h", "i");

	public static class LayerDecay implements LayerRenderer<AbstractClientPlayer> {
		private final RenderPlayer renderer;

		public LayerDecay(RenderPlayer renderer) {
			this.renderer = renderer;
		}

		@Override
		public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
			if(player.hasCapability(CapabilityRegistry.CAPABILITY_DECAY, null)) {
				IDecayCapability cap = player.getCapability(CapabilityRegistry.CAPABILITY_DECAY, null);
				if(cap.isDecayEnabled()) {
					int decay = cap.getDecayStats().getDecayLevel();
					if(decay > 0) {
						ModelPlayer model = this.renderer.getMainModel();
						boolean bipedHeadwearShow = model.bipedHeadwear.showModel;
						model.bipedHeadwear.showModel = false;
						boolean bipedRightLegwearShow = model.bipedRightLegwear.showModel;
						model.bipedRightLegwear.showModel = false;
						boolean bipedLeftLegwearShow = model.bipedLeftLegwear.showModel;
						model.bipedLeftLegwear.showModel = false;
						boolean bipedBodyWearShow = model.bipedBodyWear.showModel;
						model.bipedBodyWear.showModel = false;
						boolean bipedRightArmwearShow = model.bipedRightArmwear.showModel;
						model.bipedRightArmwear.showModel = false;
						boolean bipedLeftArmwearShow = model.bipedLeftArmwear.showModel;
						model.bipedLeftArmwear.showModel = false;

						//Render decay overlay
						float glow = (float) ((Math.cos(player.ticksExisted / 10.0D) + 1.0D) / 2.0D) * 0.15F;
						float transparency = 0.85F * decay / 20.0F - glow;
						GlStateManager.enableBlend();
						GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
						this.renderer.bindTexture(PLAYER_DECAY_TEXTURE);
						GlStateManager.color(1, 1, 1, transparency);
						model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
						GlStateManager.color(1, 1, 1, 1);

						model.bipedHeadwear.showModel = bipedHeadwearShow;
						model.bipedRightLegwear.showModel = bipedRightLegwearShow;
						model.bipedLeftLegwear.showModel = bipedLeftLegwearShow;
						model.bipedBodyWear.showModel = bipedBodyWearShow;
						model.bipedRightArmwear.showModel = bipedRightArmwearShow;
						model.bipedLeftArmwear.showModel = bipedLeftArmwearShow;
					}
				}
			}
		}

		@Override
		public boolean shouldCombineTextures() {
			return false;
		}
	}

	@SubscribeEvent
	public static void onPreRenderPlayer(RenderPlayerEvent.Pre event) {
		EntityPlayer player = event.getEntityPlayer();

		if(player.hasCapability(CapabilityRegistry.CAPABILITY_DECAY, null)) {
			IDecayCapability capability = player.getCapability(CapabilityRegistry.CAPABILITY_DECAY, null);
			if(capability.isDecayEnabled() && capability.getDecayStats().getDecayLevel() > 0) {
				try {
					@SuppressWarnings("unchecked")
					List<LayerRenderer<?>> layers = (List<LayerRenderer<?>>) fieldLayerRenderers.get(event.getRenderer());
					boolean hasLayer = false;
					for(LayerRenderer<?> layer : layers) {
						if(layer instanceof LayerDecay) {
							hasLayer = true;
							break;
						}
					}
					if(!hasLayer) {
						event.getRenderer().addLayer(new LayerDecay(event.getRenderer()));
					}
				} catch(Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onRenderHand(RenderSpecificHandEvent event) {
		GlStateManager.pushMatrix();

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		if(player != null && player.hasCapability(CapabilityRegistry.CAPABILITY_DECAY, null)) {
			IDecayCapability capability = player.getCapability(CapabilityRegistry.CAPABILITY_DECAY, null);
			if(capability.isDecayEnabled() && capability.getDecayStats().getDecayLevel() > 0) {
				int decay = capability.getDecayStats().getDecayLevel();
				boolean isMainHand = event.getHand() == EnumHand.MAIN_HAND;
				if(isMainHand && !player.isInvisible() && event.getItemStack() == null) {
					EnumHandSide enumhandside = isMainHand ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
					renderArmFirstPersonWithDecay(event.getEquipProgress(), event.getSwingProgress(), enumhandside, decay);
					event.setCanceled(true);
				}
			}
		}

		GlStateManager.popMatrix();
	}

	/**
	 * From ItemRenderer#renderArmFirstPerson
	 * @param swingProgress
	 * @param equipProgress
	 * @param handSide
	 * @param decay
	 */
	private static void renderArmFirstPersonWithDecay(float swingProgress, float equipProgress, EnumHandSide handSide, int decay) {
		Minecraft mc = Minecraft.getMinecraft();
		RenderManager renderManager = mc.getRenderManager();
		boolean flag = handSide != EnumHandSide.LEFT;
		float f = flag ? 1.0F : -1.0F;
		float f1 = MathHelper.sqrt_float(equipProgress);
		float f2 = -0.3F * MathHelper.sin(f1 * (float)Math.PI);
		float f3 = 0.4F * MathHelper.sin(f1 * ((float)Math.PI * 2F));
		float f4 = -0.4F * MathHelper.sin(equipProgress * (float)Math.PI);
		GlStateManager.translate(f * (f2 + 0.64000005F), f3 + -0.6F + swingProgress * -0.6F, f4 + -0.71999997F);
		GlStateManager.rotate(f * 45.0F, 0.0F, 1.0F, 0.0F);
		float f5 = MathHelper.sin(equipProgress * equipProgress * (float)Math.PI);
		float f6 = MathHelper.sin(f1 * (float)Math.PI);
		GlStateManager.rotate(f * f6 * 70.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(f * f5 * -20.0F, 0.0F, 0.0F, 1.0F);
		AbstractClientPlayer abstractclientplayer = mc.thePlayer;
		mc.getTextureManager().bindTexture(abstractclientplayer.getLocationSkin());
		GlStateManager.translate(f * -1.0F, 3.6F, 3.5F);
		GlStateManager.rotate(f * 120.0F, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(200.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(f * -135.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.translate(f * 5.6F, 0.0F, 0.0F);
		RenderPlayer renderplayer = (RenderPlayer)renderManager.getEntityRenderObject(abstractclientplayer);
		GlStateManager.disableCull();

		if (flag) {
			renderplayer.renderRightArm(abstractclientplayer);

			mc.renderEngine.bindTexture(PLAYER_DECAY_TEXTURE);
			GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			float glow = (float) ((Math.cos(abstractclientplayer.ticksExisted / 10.0D) + 1.0D) / 2.0D) * 0.15F;
			float transparency = 0.85F * decay / 20.0F - glow;
			GlStateManager.color(1, 1, 1, transparency);

			//From RenderPlayer#renderRightArm
			ModelPlayer modelplayer = renderplayer.getMainModel();
			GlStateManager.enableBlend();
			modelplayer.swingProgress = 0.0F;
			modelplayer.isSneak = false;
			modelplayer.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, abstractclientplayer);
			modelplayer.bipedRightArm.rotateAngleX = 0.0F;
			modelplayer.bipedRightArm.render(0.0625F);
			modelplayer.bipedRightArmwear.rotateAngleX = 0.0F;
			modelplayer.bipedRightArmwear.render(0.0625F);
			GlStateManager.disableBlend();
		} else {
			renderplayer.renderLeftArm(abstractclientplayer);

			mc.renderEngine.bindTexture(PLAYER_DECAY_TEXTURE);
			GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			float glow = (float) ((Math.cos(abstractclientplayer.ticksExisted / 10.0D) + 1.0D) / 2.0D) * 0.15F;
			float transparency = 0.85F * decay / 20.0F - glow;
			GlStateManager.color(1, 1, 1, transparency);

			//From RenderPlayer#renderLeftArm
			ModelPlayer modelplayer = renderplayer.getMainModel();
			GlStateManager.enableBlend();
			modelplayer.isSneak = false;
			modelplayer.swingProgress = 0.0F;
			modelplayer.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, abstractclientplayer);
			modelplayer.bipedLeftArm.rotateAngleX = 0.0F;
			modelplayer.bipedLeftArm.render(0.0625F);
			modelplayer.bipedLeftArmwear.rotateAngleX = 0.0F;
			modelplayer.bipedLeftArmwear.render(0.0625F);
			GlStateManager.disableBlend();
		}

		GlStateManager.color(1, 1, 1, 1);

		GlStateManager.enableCull();
	}
}

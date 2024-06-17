package mod.schnappdragon.habitat.client.renderer.entity;

import mod.schnappdragon.habitat.client.model.PookaModel;
import mod.schnappdragon.habitat.client.renderer.HabitatModelLayers;
import mod.schnappdragon.habitat.client.renderer.entity.layers.PookaGlowLayer;
import mod.schnappdragon.habitat.common.entity.animal.Pooka;
import mod.schnappdragon.habitat.core.Habitat;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class PookaRenderer extends MobRenderer<Pooka, PookaModel<Pooka>> {
    private static final ResourceLocation POOKA_TEXTURES = new ResourceLocation(Habitat.MODID, "textures/entity/pooka/pooka.png");

    public PookaRenderer(EntityRendererProvider.Context context) {
        super(context, new PookaModel<>(context.bakeLayer(HabitatModelLayers.POOKA)), 0.3F);
        this.addLayer(new PookaGlowLayer<>(this));
    }

    protected int getBlockLightLevel(Pooka entityIn, BlockPos partialTicks) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(Pooka entity) {
        return POOKA_TEXTURES;
    }
}
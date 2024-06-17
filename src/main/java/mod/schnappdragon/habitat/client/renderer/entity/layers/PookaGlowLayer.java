package mod.schnappdragon.habitat.client.renderer.entity.layers;

import mod.schnappdragon.habitat.client.model.PookaModel;
import mod.schnappdragon.habitat.common.entity.animal.Pooka;
import mod.schnappdragon.habitat.core.Habitat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public class PookaGlowLayer<T extends Pooka> extends EyesLayer<T, PookaModel<T>> {
    private static final RenderType GLOW = RenderType.eyes(new ResourceLocation(Habitat.MODID, "textures/entity/pooka/glow.png"));

    public PookaGlowLayer(RenderLayerParent<T, PookaModel<T>> renderLayerParent) {
        super(renderLayerParent);
    }

    public RenderType renderType() {
        return GLOW;
    }
}
package ProjectSteamCrafting.Items.Mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;

public class StringMesh extends ItemMesh{
    public static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("projectsteam_crafting", "textures/item/string_mesh.png");
    public ResourceLocation getTexture(){
        return texture;
    }
}

package ProjectSteamCrafting;

import ProjectSteamCrafting.Items.Mesh.StringMesh;
import ProjectSteamCrafting.Sieve.BlockSieve;
import ProjectSteamCrafting.Sieve.EntitySieve;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class Registry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Block> BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK, "projectsteam_crafting");
    public static final net.neoforged.neoforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "projectsteam_crafting");
    public static final net.neoforged.neoforge.registries.DeferredRegister<Item> ITEMS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.ITEM, "projectsteam_crafting");

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b){
        return ITEMS.register(name,() -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Block> SIEVE = BLOCKS.register(
            "sieve",
            () -> new BlockSieve()
    );
    public static final Supplier<BlockEntityType<EntitySieve>> ENTITY_SIEVE = BLOCK_ENTITIES.register(
            "entity_sieve",
            () -> BlockEntityType.Builder.of(EntitySieve::new, SIEVE.get()).build(null)
    );

public static final Supplier<Item> STRING_MESH = ITEMS.register(
        "string_mesh",
        () -> new StringMesh()
);

    static {
        registerBlockItem("sieve", SIEVE);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

}

package com.xiaofeiwu.grandwitch;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GrandWitchMod.MODID);

    public static final RegistryObject<Block> MOUSE_HOLE = BLOCKS.register("mouse_hole", MouseHoleBlock::new);

    /** What a mouse leaves where it has dug. It has no item: it is made by digging, and gives dirt. */
    public static final RegistryObject<Block> MOUSE_TUNNEL = BLOCKS.register("mouse_tunnel", MouseTunnelBlock::new);

    private ModBlocks() {
    }
}

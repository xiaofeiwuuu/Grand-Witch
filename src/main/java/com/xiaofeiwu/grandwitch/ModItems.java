package com.xiaofeiwu.grandwitch;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GrandWitchMod.MODID);

    public static final RegistryObject<Item> GRAND_WITCH_SPAWN_EGG = ITEMS.register("grand_witch_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GRAND_WITCH, 0x4A2C6B, 0x9CB878, new Item.Properties()));

    public static final RegistryObject<Item> MOUSE_SPAWN_EGG = ITEMS.register("mouse_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.VILLAGE_MOUSE, 0x969098, 0xE29AA4, new Item.Properties()));

    public static final RegistryObject<Item> MOUSE_HOLE = ITEMS.register("mouse_hole", () -> new net.minecraft.world.item.BlockItem(ModBlocks.MOUSE_HOLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ANTIDOTE = ITEMS.register("antidote", () -> new AntidoteItem(new Item.Properties()));

    public static final RegistryObject<Item> PLAGUE_VIRUS = ITEMS.register("plague_virus", () -> new Item(new Item.Properties()) {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level, java.util.List<net.minecraft.network.chat.Component> tips, net.minecraft.world.item.TooltipFlag flag) {
            tips.add(net.minecraft.network.chat.Component.translatable("item.grandwitch.plague_virus.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    });
    public static final RegistryObject<Item> WOODEN_BROOM = ITEMS.register("wooden_broom", () -> new BroomItem(BroomKind.WOOD, new Item.Properties()));
    public static final RegistryObject<Item> GOLDEN_BROOM = ITEMS.register("golden_broom", () -> new BroomItem(BroomKind.GOLD, new Item.Properties()));

    private ModItems() {
    }
}

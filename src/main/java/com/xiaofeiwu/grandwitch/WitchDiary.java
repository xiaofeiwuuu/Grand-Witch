package com.xiaofeiwu.grandwitch;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The witch's diary: a written book, in her voice, with what she fears and how she is to be seen through, and the way to make the antidote. It is kept in the
 * chest of her house. Its pages are text that is translated where it is read, so it is in the language of whoever opens it.
 */
public final class WitchDiary {

    static final int PAGES = 9;

    private WitchDiary() {
    }

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "A Witch's Diary");
        tag.putString("author", "The Grand Witch");
        tag.putInt("generation", 3);                         // as worn as a book can be, and not to be copied
        ListTag pages = new ListTag();
        for (int i = 1; i <= PAGES; i++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.grandwitch.diary." + i))));
        }
        tag.put("pages", pages);
        return book;
    }
}

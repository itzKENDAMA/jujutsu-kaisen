package radon.jujutsu_kaisen.world.gen.processor.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.item.JJKItems;
import radon.jujutsu_kaisen.tags.JJKItemTags;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public abstract class CursedToolItemFrameProcessor extends StructureProcessor {
    private static ItemStack getRandomCursedTool(ServerLevel level) {
        InvertedSpearData data = level.getDataStorage().computeIfAbsent(InvertedSpearData::load, InvertedSpearData::new,
                InvertedSpearData.IDENTIFIER);

        List<ItemStack> pool = new ArrayList<>();

        Registry<Item> registry = level.registryAccess().registryOrThrow(Registries.ITEM);

        for (Item item : registry) {
            ItemStack stack = new ItemStack(item);

            if (stack.is(JJKItemTags.CURSED_TOOL)) {
                pool.add(stack);
            }
        }
        pool.removeIf(x -> data.exists && x.is(JJKItems.INVERTED_SPEAR_OF_HEAVEN.get()));

        ItemStack stack = pool.get(HelperMethods.RANDOM.nextInt(pool.size()));

        if (stack.is(JJKItems.INVERTED_SPEAR_OF_HEAVEN.get())) {
            data.exists = true;
            data.setDirty();
        }
        return stack;
    }

    @Override
    public StructureTemplate.@NotNull StructureEntityInfo processEntity(@NotNull LevelReader world, @NotNull BlockPos seedPos, StructureTemplate.@NotNull StructureEntityInfo rawEntityInfo, StructureTemplate.@NotNull StructureEntityInfo entityInfo, @NotNull StructurePlaceSettings placementSettings, @NotNull StructureTemplate template) {
        ServerLevel level = ((ServerLevelAccessor) world).getLevel();

        Optional<Entity> entity = EntityType.create(entityInfo.nbt, level);

        if (entity.isPresent()) {
            if (entity.get() instanceof ItemFrame frame) {
                frame.setItem(getRandomCursedTool(level), false);
                return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, frame.serializeNBT());
            }
        }
        return super.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
    }

    public static class InvertedSpearData extends SavedData {
        public static final String IDENTIFIER = "inverted_spear_data";

        private boolean exists;

        public static InvertedSpearData load(CompoundTag pCompoundTag) {
            InvertedSpearData data = new InvertedSpearData();
            data.exists = pCompoundTag.getBoolean("exists");
            return data;
        }

        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag pCompoundTag) {
            pCompoundTag.putBoolean("exists", this.exists);
            return pCompoundTag;
        }
    }
}

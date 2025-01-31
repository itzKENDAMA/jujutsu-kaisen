package radon.jujutsu_kaisen.block.fluid;

import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.block.base.JJKFluidType;

public class JJKFluidTypes {
    public static DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, JujutsuKaisen.MOD_ID);

    public static final RegistryObject<FluidType> CHIMERA_SHADOW_GARDEN = FLUID_TYPES.register("chimera_shadow_garden",
            () -> new JJKFluidType(0x141414, FluidType.Properties.create()
                    .canSwim(false)
                    .motionScale(1.0D)));
    public static final RegistryObject<FluidType> FAKE_WATER = FLUID_TYPES.register("fake_water",
            () -> new JJKFluidType(0xFF3F76E4, FluidType.Properties.create()
                    .fallDistanceModifier(0.0F)
                    .canExtinguish(true)
                    .canConvertToSource(true)
                    .supportsBoating(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                    .canHydrate(true)));
}

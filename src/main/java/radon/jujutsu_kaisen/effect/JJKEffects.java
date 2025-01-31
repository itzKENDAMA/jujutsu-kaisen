package radon.jujutsu_kaisen.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import radon.jujutsu_kaisen.JujutsuKaisen;

public class JJKEffects {
    public static DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, JujutsuKaisen.MOD_ID);

    public static RegistryObject<MobEffect> STUN = EFFECTS.register("stun", () -> new JJKEffect(MobEffectCategory.HARMFUL, 0xFFFFFF));
    public static RegistryObject<MobEffect> UNLIMITED_VOID = EFFECTS.register("unlimited_void", () -> new JJKEffect(MobEffectCategory.HARMFUL, 0x000000));
    public static RegistryObject<MobEffect> UNDETECTABLE = EFFECTS.register("undetectable", () -> new JJKEffect(MobEffectCategory.BENEFICIAL, 0x000000));
    public static RegistryObject<MobEffect> CURSED_BUD = EFFECTS.register("cursed_bud", () -> new JJKEffect(MobEffectCategory.HARMFUL, 0x00FF00));
    public static RegistryObject<MobEffect> TWENTY_FOUR_FRAME_RULE = EFFECTS.register("twenty_four_frame_rule", () -> new TwentyFourFrameRuleEffect(MobEffectCategory.HARMFUL, 0xFFFFFF));
}

package net.backupcup.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.backupcup.hexed.Hexed;
import net.backupcup.hexed.entity.blazingSkull.BlazingSkullEntity;
import net.backupcup.hexed.register.*;
import net.backupcup.hexed.util.HexHelper;
import net.backupcup.hexed.util.ItemUseCooldown;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.Random;

@Mixin(value = LivingEntity.class, priority = 10)
public abstract class LivingEntityMixin extends Entity{

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Unique private boolean isFlaring = false;

    @Unique private boolean hasSpawnedSkulls = false;

    @Shadow public abstract float getHealth();

    @Shadow public abstract boolean hasStatusEffect(StatusEffect effect);

    @Shadow public abstract ItemStack getEquippedStack(EquipmentSlot slot);

    @Shadow public abstract Iterable<ItemStack> getArmorItems();

    @Shadow public abstract float getMaxHealth();

    @Shadow @Nullable public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);

    @Shadow public abstract boolean addStatusEffect(StatusEffectInstance effect);

    @Shadow public abstract void damageArmor(DamageSource source, float amount);

    @Shadow public abstract boolean damage(DamageSource source, float amount);

    @Shadow public abstract void tickRiding();

    @Shadow public abstract boolean isUsingRiptide();

    @Shadow public abstract ItemStack getMainHandStack();

    @Shadow public abstract void writeCustomDataToNbt(NbtCompound nbt);

    @Shadow public abstract void readCustomDataFromNbt(NbtCompound nbt);

    @Unique
    private void entityMultiplyingEffect(StatusEffect effect, int duration, int decayLength) {
        if (hasStatusEffect(effect)) {
            int effectAmplifier = getStatusEffect(effect).getAmplifier() + 1;

            for (int i = 0; i <= effectAmplifier; i++) {
                addStatusEffect(new StatusEffectInstance(
                        effect,
                        duration + (effectAmplifier - i) * decayLength, i,
                        true, false, true
                ));
            }
        } else {
            addStatusEffect(new StatusEffectInstance(
                    effect,
                    duration, 0,
                    true, false, true
            ));
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void hexed$LivingEntityWriteData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("isFlaring", this.isFlaring);
        nbt.putBoolean("hasSpawnedSkulls", this.hasSpawnedSkulls);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void hexed$LivingEntityReadData(NbtCompound nbt, CallbackInfo ci) {
        this.isFlaring = nbt.getBoolean("isFlaring");
        this.hasSpawnedSkulls = nbt.getBoolean("hasSpawnedSkulls");
    }

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void hexed$cancelHeal(float amount, CallbackInfo callbackInfo) {
        if(this.hasStatusEffect(RegisterStatusEffects.INSTANCE.getSMOULDERING()) ||
                this.hasStatusEffect(RegisterStatusEffects.INSTANCE.getTRAITOROUS()) ||
                HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.CHEST), RegisterEnchantments.INSTANCE.getBLOODTHIRSTY_HEX())) {

            if(HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.CHEST), RegisterEnchantments.INSTANCE.getBLOODTHIRSTY_HEX())
                    && HexHelper.INSTANCE.hasFullRobes(getArmorItems()) && getMaxHealth()/getHealth() > 1.3) return;
            callbackInfo.cancel();
        }
    }

    @ModifyArg(method = "handleFallDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), index = 1)
    private float hexed$fallDamageMultiplier(float amount) {
        if ((HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.FEET), RegisterEnchantments.INSTANCE.getAQUATIQUE_HEX()) ||
                HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.FEET), RegisterEnchantments.INSTANCE.getDYNAMIQUE_HEX())) &&
                !HexHelper.INSTANCE.hasFullRobes(getArmorItems())) { return amount * 1.5f; }
        return amount;
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float hexed$PersecutedDamage(float amount, DamageSource source) {
        if (source.getSource() instanceof LivingEntity) {
            if (HexHelper.INSTANCE.hasEnchantmentInSlot(((LivingEntity) source.getSource()).getMainHandStack(), RegisterEnchantments.INSTANCE.getPERSECUTED_HEX())) {
                float healthCap = Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getPersecutedHex().getHealthCap() : 25f;
                return (amount <= healthCap) ? (amount * 0.01f * getHealth()) : healthCap;
            }
        }
        return amount;
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float hexed$EphemeralDischarge(float amount, DamageSource source) {
        if(source.getSource() instanceof LivingEntity) {
            if(HexHelper.INSTANCE.hasEnchantmentInSlot(((LivingEntity) source.getSource()).getMainHandStack(), RegisterEnchantments.INSTANCE.getEPHEMERAL_HEX())) {
                if (((LivingEntity) source.getSource()).hasStatusEffect(RegisterStatusEffects.INSTANCE.getEXHAUSTION())) {

                    float exhaustionModifier = Objects.requireNonNull(((LivingEntity) source.getSource()).getStatusEffect(RegisterStatusEffects.INSTANCE.getEXHAUSTION())).getAmplifier();
                    return Math.max(0, amount - exhaustionModifier);
                }
            }
        }
        return amount;
    }

    @ModifyVariable(method = "travel",
        at = @At("STORE"), ordinal = 2)
    private float hexed$AquatiqueSpeed(float value) {
        if (HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.FEET), RegisterEnchantments.INSTANCE.getAQUATIQUE_HEX())) {
            return (value + 1) * (Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getAquatiqueHex().getSpeedMultiplier() : 2);
        }
        return value;
    }

    @ModifyExpressionValue(method = "travel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isOnGround()Z", ordinal = 0))
    private boolean hexed$AquatiqueCap(boolean original) {
        return original && !HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.FEET), RegisterEnchantments.INSTANCE.getAQUATIQUE_HEX());
    }
    @Inject(method = "heal", at = @At("HEAD"))
    private void hexed$MetamorphosisFood(float amount, CallbackInfo ci) {
        if (getMaxHealth() < getHealth() + amount
            && HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.HEAD), RegisterEnchantments.INSTANCE.getMETAMORPHOSIS_HEX())
            && (Object)this instanceof PlayerEntity) {

            PlayerEntity player = (PlayerEntity) (Object) this;
            amount -= getMaxHealth() - getHealth();
            player.getHungerManager().add(
                    (int) (amount * (Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getMetamorphosisHex().getFoodModifier() : 1)),
                    Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getMetamorphosisHex().getSaturationAmount() : 0f);
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float hexed$IroncladDamage(float amount, DamageSource source) {
        if (HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.LEGS), RegisterEnchantments.INSTANCE.getIRONCLAD_HEX())) {
            int duration = Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getIroncladHex().getDebuffDuration() : 200;
            if (HexHelper.INSTANCE.hasFullRobes(getArmorItems()))
                duration /= Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getIroncladHex().getRobesDebuffModifier() : 2;

            if (getHealth() < getMaxHealth() && amount >= 1) {
                entityMultiplyingEffect(RegisterStatusEffects.INSTANCE.getIRONCLAD(),
                        duration,
                        Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getIroncladHex().getDebuffDecayLength() : 10);
            }
            return amount * (1f - (Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getIroncladHex().getDamageReductionAmount() : 0.33f));
        }
        return amount;
    }

    @Inject(method = "getJumpVelocity", at = @At("RETURN"), cancellable = true)
    private void hexed$DynamiqueJump(CallbackInfoReturnable<Float> cir) {
        float DynamiqueJumpModifier = HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.FEET), RegisterEnchantments.INSTANCE.getDYNAMIQUE_HEX()) ?
                (Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getDynamiqueHex().getJumpModifier() : 0.15625f) : 0f;
        cir.setReturnValue(cir.getReturnValue() + DynamiqueJumpModifier);
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float hexed$AvertingDamage(float amount, DamageSource source) {
        int avertingArmor = 0;
        for (ItemStack itemStack : getArmorItems()) {
            if(EnchantmentHelper.get(itemStack).containsKey(RegisterEnchantments.INSTANCE.getAVERTING_HEX())) avertingArmor += 1;
        }

        if((1-Math.exp(-avertingArmor)) * (1-amount/getMaxHealth()) > new Random().nextFloat()) {
            float newAmount = amount * (1 - (Hexed.INSTANCE.getConfig() != null ? (Hexed.INSTANCE.getConfig().getAvertingHex().getDamageReduction()) : 0.125f)*avertingArmor);
            damageArmor(source, newAmount);
            return newAmount;
        }

        return amount;
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float hexed$FranticDamage(float amount, DamageSource source) {
        if (source.isOf(RegisterDamageTypes.INSTANCE.getFRANTIC_DAMAGE())) return amount;

        if (HexHelper.INSTANCE.hasEnchantmentInSlot(getEquippedStack(EquipmentSlot.LEGS), RegisterEnchantments.INSTANCE.getFRANTIC_HEX())) {
            entityMultiplyingEffect(RegisterStatusEffects.INSTANCE.getFRANTIC(),
                    Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getFranticHex().getFranticDuration() : 100,
                    Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getFranticHex().getFranticDecayLength() : 50);

            if (HexHelper.INSTANCE.hasFullRobes(getArmorItems())) return amount;

            damage(RegisterDamageTypes.INSTANCE.of(getWorld(), RegisterDamageTypes.INSTANCE.getFRANTIC_DAMAGE()),
                    amount *= 1 + (float) getStatusEffect(RegisterStatusEffects.INSTANCE.getFRANTIC()).getAmplifier() /
                            (Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getFranticHex().getDamageModifier() : 2f));
            return 0;
        }

        return amount;
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hexed$FlaringRiptide(CallbackInfo ci) {
        if (isUsingRiptide() && HexHelper.INSTANCE.hasEnchantmentInSlot(getMainHandStack(), RegisterEnchantments.INSTANCE.getFLARING_HEX()) && !this.isFlaring) {
            this.isFlaring = true;
        }
        if(!isUsingRiptide() && this.isFlaring) this.isFlaring = false;

        if (this.isFlaring) {
            getWorld().spawnEntity(
                    FallingBlockEntity.spawnFromBlock(getWorld(), ((PlayerEntity) (Object) this).getBlockPos(),
                            (Hexed.INSTANCE.getConfig() != null ?
                                Hexed.INSTANCE.getConfig().getFlaringHex().isSoulFire() ?
                                    Blocks.SOUL_FIRE.getDefaultState() :
                                    Blocks.FIRE.getDefaultState() :
                                Blocks.FIRE.getDefaultState()
                            )
                    )
            );
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hexed$SepultureRiptide(CallbackInfo ci) {
        if (isUsingRiptide() && HexHelper.INSTANCE.hasEnchantmentInSlot(getMainHandStack(), RegisterEnchantments.INSTANCE.getSEPULTURE_HEX()) && !this.hasSpawnedSkulls) {
            this.hasSpawnedSkulls = true;

            for (int i = 0; i < 3; i++) {
                double angle = i * 2 * Math.PI / 3;
                Vec3d spawnPos = new Vec3d(getPos().getX() + 1 * Math.cos(angle), getPos().getY() + 0.5, getPos().getZ() + 1 * Math.sin(angle));
                Vec3d movementVec = new Vec3d((spawnPos.x - getPos().getX())/4, 0.333, (spawnPos.z - getPos().getZ())/4);

                LivingEntity entity = kotlin.random.Random.Default.nextDouble(0.0, 1.0) <=
                        (Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getSepultureHex().getAngerChance() : 0.333) ?
                        (LivingEntity) (Object) this : null;
                if(HexHelper.INSTANCE.hasFullRobes(getArmorItems()) && entity != null) entity = null;

                float explosionPower = Hexed.INSTANCE.getConfig() != null ? Hexed.INSTANCE.getConfig().getSepultureHex().getExplosionPower() : 1.5f;

                getWorld().spawnEntity(
                        new BlazingSkullEntity(
                                RegisterEntities.INSTANCE.getBLAZING_SKULL(), getWorld(),
                                spawnPos, movementVec, entity, explosionPower)
                );
            }

            getWorld().playSound(
                    null, getBlockPos(),
                    RegisterSounds.INSTANCE.getACCURSED_ALTAR_HEX(), SoundCategory.HOSTILE,
                    (float) kotlin.random.Random.Default.nextDouble(0.5, 1.0),
                    (float) kotlin.random.Random.Default.nextDouble(0.75, 1.25)
            );
        }
        if(!isUsingRiptide() && this.hasSpawnedSkulls) this.hasSpawnedSkulls = false;
    }
}

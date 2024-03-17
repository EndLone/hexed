package net.backupcup.mixin;

import net.backupcup.hexed.entity.blazingSkull.BlazingSkullEntity;
import net.backupcup.hexed.packets.HexNetworkingConstants;
import net.backupcup.hexed.register.RegisterEnchantments;
import net.backupcup.hexed.register.RegisterEntities;
import net.backupcup.hexed.register.RegisterSounds;
import net.backupcup.hexed.register.RegisterStatusEffects;
import net.backupcup.hexed.util.HexHelper;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity {


    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique private Entity hitEntity = null;

    @Shadow private ItemStack tridentStack;

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void hexed$LingerAura(CallbackInfo ci) {
        if (!inGround) return;

        if (HexHelper.INSTANCE.hasEnchantmentInSlot(tridentStack, RegisterEnchantments.INSTANCE.getLINGER_HEX())) {
            int radius = 3;
            TridentEntity tridentEntity = (TridentEntity) (Object) this;

            if (getWorld().getTime() % 20 == 0) {
                for (Entity entity : getWorld().getOtherEntities(null, getBoundingBox().expand(radius))) {
                    if (!(entity instanceof LivingEntity)) continue;

                    ((LivingEntity) entity).addStatusEffect(new StatusEffectInstance(
                            RegisterStatusEffects.INSTANCE.getABLAZE(),
                            25, 0, true, false, true
                    ));

                    if (HexHelper.INSTANCE.hasFullRobes(tridentEntity.getOwner().getArmorItems())) continue;
                    tridentStack.damage(1, Random.create(), null);
                    if (tridentStack.getDamage() == tridentStack.getMaxDamage()) {
                        getWorld().playSound(null, tridentEntity.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS);
                        tridentEntity.discard();
                    }
                }
            }

            Vec3d pos = new Vec3d(
                    getX() + (random.nextDouble() * 2 - 1) * radius,
                    getY() + random.nextDouble() * radius,
                    getZ() + (random.nextDouble() * 2 - 1) * radius);

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);

            for (ServerPlayerEntity player : PlayerLookup.tracking(tridentEntity)) {
                ServerPlayNetworking.send(player, HexNetworkingConstants.INSTANCE.getLINGER_PARTICLE_PACKET(), buf);
            }
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void hexed$FlaringSummonFire(CallbackInfo ci) {
        if (inGround) return;

        if (HexHelper.INSTANCE.hasEnchantmentInSlot(tridentStack, RegisterEnchantments.INSTANCE.getFLARING_HEX())) {
            TridentEntity tridentEntity = (TridentEntity) (Object) this;

            getWorld().spawnEntity(
                    FallingBlockEntity.spawnFromBlock(getWorld(), tridentEntity.getBlockPos(), Blocks.FIRE.getDefaultState())
            );
        }
    }

    @Inject(method = "onEntityHit", at = @At(value = "HEAD"))
    private void hexed$SeizeGetEntity(EntityHitResult entityHitResult, CallbackInfo ci) {
        if (HexHelper.INSTANCE.hasEnchantmentInSlot(tridentStack, RegisterEnchantments.INSTANCE.getSEIZE_HEX())) {
            this.hitEntity = entityHitResult.getEntity();
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void hexed$SeizePull(CallbackInfo ci) {
        if (!(this.hitEntity instanceof LivingEntity) || this.hitEntity == null) return;
        TridentEntity tridentEntity = (TridentEntity) (Object) this;

        int maxPullableHP = HexHelper.INSTANCE.hasFullRobes(tridentEntity.getOwner().getArmorItems()) ? 100 : 50;

        if (((LivingEntity) this.hitEntity).getMaxHealth() > maxPullableHP || ((LivingEntity) this.hitEntity).isBlocking()) return;

        if (EnchantmentHelper.getLoyalty(tridentStack) > 0) {
            hitEntity.setPos(tridentEntity.getPos().x, tridentEntity.getPos().y - this.hitEntity.getHeight()/2, tridentEntity.getPos().z);
        } else {
            Vec3d pullDirection = tridentEntity.getOwner().getPos().subtract(this.hitEntity.getPos()).normalize();

            double distance = tridentEntity.getOwner().getPos().distanceTo(this.hitEntity.getPos());
            double maxDistance = distance/2;
            Pair<Double, Double> pullStrengthBound = new Pair<>(1.0, 4.0);
            double pullStrength = pullStrengthBound.getLeft() + (pullStrengthBound.getRight() - pullStrengthBound.getLeft()) * (1 - Math.min(distance / maxDistance, 1));

            this.hitEntity.addVelocity(pullDirection.multiply(pullStrength));
            this.hitEntity = null;
        }
    }

    @Inject(method = "onEntityHit", at = @At(value = "HEAD"))
    private void hexed$SepultureGetEntity(EntityHitResult entityHitResult, CallbackInfo ci) {
        if (HexHelper.INSTANCE.hasEnchantmentInSlot(tridentStack, RegisterEnchantments.INSTANCE.getSEPULTURE_HEX()) &&
                entityHitResult.getEntity() instanceof LivingEntity) {
            if (entityHitResult.getEntity().getType() == RegisterEntities.INSTANCE.getBLAZING_SKULL()) return;

            for (int i = 0; i < 3; i++) {
                double angle = i * 2 * Math.PI / 3;
                Vec3d spawnPos = new Vec3d(getPos().getX() + 1 * Math.cos(angle), getPos().getY() + 0.5, getPos().getZ() + 1 * Math.sin(angle));
                Vec3d movementVec = new Vec3d((spawnPos.x - getPos().getX())/4, 0.333, (spawnPos.z - getPos().getZ())/4);

                LivingEntity entity = kotlin.random.Random.Default.nextDouble(0.0, 1.0) <= 0.333 ? (LivingEntity) getOwner() : null;
                if(HexHelper.INSTANCE.hasFullRobes(getOwner().getArmorItems()) && entity != null) entity = null;

                getWorld().spawnEntity(
                        new BlazingSkullEntity(
                                RegisterEntities.INSTANCE.getBLAZING_SKULL(), getWorld(),
                                spawnPos, movementVec, entity)
                );
            }

            getWorld().playSound(
                    null, getBlockPos(),
                    RegisterSounds.INSTANCE.getACCURSED_ALTAR_HEX(), SoundCategory.HOSTILE,
                    (float) kotlin.random.Random.Default.nextDouble(0.5, 1.0),
                    (float) kotlin.random.Random.Default.nextDouble(0.75, 1.25)
            );
        }
    }
}

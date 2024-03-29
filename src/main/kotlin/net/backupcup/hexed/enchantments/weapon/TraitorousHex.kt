package net.backupcup.hexed.enchantments.weapon

import net.backupcup.hexed.Hexed
import net.backupcup.hexed.enchantments.AbstractHex
import net.backupcup.hexed.register.RegisterStatusEffects
import net.backupcup.hexed.util.HexHelper
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import kotlin.math.abs

class TraitorousHex(
    weight: Rarity?,
    target: EnchantmentTarget?,
    slotTypes: Array<out EquipmentSlot>?,
    texturepath: Identifier
) : AbstractHex(
    weight,
    target,
    slotTypes,
    texturepath) {
    override fun onTargetDamaged(user: LivingEntity, target: Entity, level: Int) {
        var targetYaw = target.yaw % 360
        if (targetYaw > 180) targetYaw -= 360

        var userYaw = user.yaw % 360
        if (userYaw > 180) userYaw -= 360

        if (abs(targetYaw - userYaw) <= 67.5 && target is LivingEntity && user is PlayerEntity) {
            target.addStatusEffect(
                StatusEffectInstance(
                    RegisterStatusEffects.TRAITOROUS,
                    Hexed.getConfig()?.traitorousHex?.debuffDuration ?: 80, 0,
                    true, true, true
                ))

            if (!HexHelper.hasFullRobes(user)) {
                if (HexHelper.getEnchantments(user.mainHandStack).contains(this))
                    user.itemCooldownManager.set(user.mainHandStack.item, Hexed.getConfig()?.traitorousHex?.cooldownDuration ?: 40)
            }
        }

        super.onTargetDamaged(user, target, level)
    }
}
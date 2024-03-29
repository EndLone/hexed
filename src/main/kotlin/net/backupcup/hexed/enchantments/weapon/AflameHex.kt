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
import net.minecraft.util.Identifier

class AflameHex(
    weight: Rarity?,
    target: EnchantmentTarget?,
    slotTypes: Array<out EquipmentSlot>?,
    texturepath: Identifier
) : AbstractHex(
    weight,
    target,
    slotTypes,
    texturepath
) {

    override fun onTargetDamaged(user: LivingEntity, target: Entity, level: Int) {
        if(target is LivingEntity) {
            if (target.isAlive) {
                target.addStatusEffect(StatusEffectInstance(
                    RegisterStatusEffects.ABLAZE,
                    Hexed.getConfig()?.aflameHex?.AblazeDuration ?: 80, Hexed.getConfig()?.aflameHex?.AblazeAmplifier ?: 1,
                    true, true, true
                ))
            }
            if (user.isAlive && !HexHelper.hasFullRobes(user)) {
                user.addStatusEffect(StatusEffectInstance(
                    RegisterStatusEffects.AFLAME,
                    Hexed.getConfig()?.aflameHex?.AflameDuration ?: 40, Hexed.getConfig()?.aflameHex?.AflameAmplifier ?: 0,
                    true, true, true
                ))
            }
        }
    }
}
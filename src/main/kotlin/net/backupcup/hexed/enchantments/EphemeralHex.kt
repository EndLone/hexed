package net.backupcup.hexed.enchantments

import net.backupcup.hexed.register.RegisterStatusEffects
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityGroup
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity

class EphemeralHex(
    weight: Rarity?,
    target: EnchantmentTarget?,
    slotTypes: Array<out EquipmentSlot>?
) : AbstractHex(
    weight,
    target,
    slotTypes
) {
    override fun getAttackDamage(level: Int, group: EntityGroup?): Float {
        return 4f
    }

    override fun onTargetDamaged(user: LivingEntity, target: Entity, level: Int) {
        if (target is LivingEntity) {
            if(!hasFullRobes(user)) {
                entityMultiplyingEffect(user, RegisterStatusEffects.EXHAUSTION, 150, 50)
            }
        }
    }
}
package net.backupcup.hexed

import net.backupcup.hexed.datagen.DatagenLoot
import net.backupcup.hexed.datagen.DatagenRecipes
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

object HexedDataGenerator : DataGeneratorEntrypoint {
	override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
		val pack: FabricDataGenerator.Pack = fabricDataGenerator.createPack()

		pack.addProvider(::DatagenRecipes)
		pack.addProvider(::DatagenLoot)
	}
}
package com.rhseung.abstractlib.api.registration.registrykeys

import com.rhseung.abstractlib.api.registration.RegistryManager
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.resource.featuretoggle.FeatureFlag
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity

class BasicItem private constructor(
	val id: Identifier,
	private val setting: Settings,
) : Item(setting.settings), IBasicRegistryKey {
	
	init {
		RegistryManager.register(this, id, setting.group)
	}
	
	override fun toString(): String {
		return "BasicItem(id=$id, settings=$setting)"
	}
	
	companion object {
		fun of(modId: String, lambda: ItemBuilder.() -> Unit) = ItemBuilder(modId).apply(lambda).build()
	}
	
	class ItemBuilder(val modId: String) {
		var path = ""
		private var settings = Settings()
		
		fun settings(lambda: SettingsBuilder.() -> Unit): ItemBuilder {
			this.settings = SettingsBuilder().apply(lambda).build()
			return this
		}
		
		fun build() = BasicItem(Identifier(modId, path), settings)
	}
	
	class SettingsBuilder {
		var group: ItemGroup? = null
		var maxCount: Int = 64
		var maxDamage: Int = 0
		var recipeRemainder: Item? = null
		var rarity: Rarity = Rarity.COMMON
		var fireproof: Boolean = false
		var requireFeatures: Array<out FeatureFlag> = arrayOf()
		
		fun build(): Settings {
			var settings = Settings().settings
				.maxCount(maxCount)
				.maxDamage(maxDamage)
				.recipeRemainder(recipeRemainder)
				.rarity(rarity)
			
			if (requireFeatures.isNotEmpty()) settings = settings.requires(*requireFeatures)
			if (fireproof) settings = settings.fireproof()
			
			return Settings(settings, group)
		}
	}
	
	data class Settings(val settings: Item.Settings = Item.Settings(), val group: ItemGroup? = null)
}
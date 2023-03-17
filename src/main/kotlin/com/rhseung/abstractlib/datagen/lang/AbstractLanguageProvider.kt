package net.rhseung.rhseungslib.datagen.lang

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider

abstract class AbstractLanguageProvider (
	val output: FabricDataOutput,
	val languageCode: String = "en_us"
): FabricLanguageProvider(output, languageCode) {
	
	override fun generateTranslations(translationBuilder: TranslationBuilder) {
		val handler = LanguageHandler(output.modId, translationBuilder)
		register(handler)
	}
	
	open fun register(handler: LanguageHandler) {
		RegistryHelper.getItems(BasicItem::class).forEach { handler.generate(it) }
		RegistryHelper.getBlocks(BasicBlock::class).forEach { handler.generate(it) }
	}
}
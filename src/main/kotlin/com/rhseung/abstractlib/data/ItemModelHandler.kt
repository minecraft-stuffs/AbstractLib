package com.rhseung.abstractlib.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.rhseung.abstractlib.api.file.Parents
import com.rhseung.abstractlib.api.file.TextureType
import com.rhseung.abstractlib.registration.BasicItem
import net.minecraft.data.client.ItemModelGenerator
import net.minecraft.util.Identifier
import java.util.function.BiConsumer
import java.util.function.Supplier

class ItemModelHandler(
    val modId: String,
    val generator: ItemModelGenerator
) {
    fun simple(item: BasicItem, path: String = item.id.path) {
        this.generate(builder {
            model(item.id.path) {
                parent { Parents.GENERATED }
                textures {
                    + path
                }
            }
        })
    }

    data class Model(val id: Identifier, var parent: Identifier, val textures: MutableList<Texture>)

    data class Texture(var type: String, val id: Identifier)

    data class Override(val predicates: MutableMap<Identifier, Number>, val model: Model)

    /**
     * ```
     * // registry id = reimagined:gear/hoe
     * // model id    = reimagined:item/gear/hoe
     *
     * {
     *   "parent": "minecraft:item/handheld",
     *   "overrides": [
     *     {
     *       "model": "reimagined:item/gear/hoe_broken",
     *       "predicate": {
     *         "reimagined:broken": 1
     *       }
     *     },
     *     {
     *       "model": "reimagined:item/gear/hoe_grip",
     *       "predicate": {
     *         "reimagined:grip": 1
     *       }
     *     }
     *   ],
     *   "textures": {
     *     "layer0": "reimagined:item/gear/hoe/handle",
     *     "layer1": "reimagined:item/gear/hoe/hoehead",
     *     "layer2": "reimagined:item/gear/hoe/binding"
     *   }
     * }
     * ```
     * ->
     * ```
     * handler.builder
     * ```
     */
    fun builder(lambda: Builder.() -> Unit): Builder {
        return Builder(modId).apply(lambda)
    }

    class Builder(val modId: String) {
        lateinit var model: Model
        var overrides = mutableListOf<Override>()

        fun model(path: String, lambda: ModelBuilder.() -> Unit): Builder {
            val model = ModelBuilder(Identifier(modId, "item/$path"), modId).apply(lambda).build()
            if (model.parent == Parents.EMPTY)
                model.parent = Parents.GENERATED

            this.model = model
            return this
        }

        fun overrides(lambda: OverrideListBuilder.() -> Unit): Builder {
            val overrides = OverrideListBuilder(modId).apply(lambda).build()
            overrides.forEach {
                if (it.model.parent == Parents.EMPTY)
                    it.model.parent = model.parent
            }

            this.overrides = overrides
            return this
        }
    }

    class ModelBuilder(val id: Identifier, val modId: String) {
        private var parent = Parents.EMPTY
        private val textures = mutableListOf<Texture>()

        fun parent(lambda: () -> Identifier): ModelBuilder {
            parent = lambda()
            return this
        }

        fun textures(lambda: TextureListBuilder.() -> Unit): ModelBuilder {
            textures.addAll(TextureListBuilder(modId).apply(lambda).build())
            return this
        }

        fun build() = Model(id, parent, textures)
    }

    class TextureListBuilder(val modId: String) {
        private val textureList = mutableListOf<Texture>()

        operator fun String.unaryPlus() {
            textureList.add(Texture(TextureType.LAYER(textureList.count()), Identifier(modId, "item/$this")))
        }

        operator fun Pair<String, String>.unaryPlus() {
            textureList.add(Texture(first, Identifier(modId, "item/$second")))
        }

        operator fun Texture.unaryPlus() {
            textureList.add(this)
        }

        fun texture(lambda: () -> Pair<String, String>): TextureListBuilder {
            val texture = Texture(lambda().first, Identifier(modId, "item/" + lambda().second))
            if (texture.type.isBlank())
                texture.type = TextureType.LAYER(textureList.count())

            textureList.add(texture)
            return this
        }

        /**
         * ```
         * from (override.model.textures) { texture -> texture.type to texture.id }
         * from (override.model.textures) { (type, id) -> type to id }
         * from (override.model.textures) { it.type to it.id } ```
         */
        fun from(collection: Collection<Texture>, lambda: (Texture) -> Texture): TextureListBuilder {
            collection.forEach {
                textureList.add(lambda(it))
            }
            return this
        }

        fun build() = textureList
    }

    class OverrideBuilder(val modId: String) {
        var predicates = mutableMapOf<Identifier, Number>()
        lateinit var model: Model

        fun predicate(lambda: () -> Pair<String, Number>): OverrideBuilder {
            this.predicates[Identifier(modId, lambda().first)] = lambda().second
            return this
        }

        fun model(path: String, lambda: ModelBuilder.() -> Unit): OverrideBuilder {
            this.model = ModelBuilder(Identifier(modId, "item/$path"), modId).apply(lambda).build()
            return this
        }

        fun build() = Override(predicates, model)
    }

    class OverrideListBuilder(val modId: String) {
        private val overrideList = mutableListOf<Override>()

        fun override(lambda: OverrideBuilder.() -> Unit): OverrideListBuilder {
            overrideList.add(OverrideBuilder(modId).apply(lambda).build())
            return this
        }

        fun build() = overrideList
    }

    private fun BiConsumer<Identifier, Supplier<JsonElement>>.accept(builder: Builder): Identifier {
        this.accept(builder.model.id, Supplier {
            val jsonObject = JsonObject()

            jsonObject.addProperty("parent", builder.model.parent.toString())

            if (builder.model.textures.isNotEmpty()) {
                val textureJsonObject = JsonObject()
                builder.model.textures.forEach { (textureKey, textureValue) ->
                    textureJsonObject.addProperty(textureKey, textureValue.toString())
                }
                jsonObject.add("textures", textureJsonObject)
            }

            if (builder.overrides.isNotEmpty()) {
                val overrideJsonArray = JsonArray()
                builder.overrides.forEach { override ->
                    val eachOverride = JsonObject()

                    val eachPredicate = JsonObject()
                    override.predicates.forEach {
                        eachPredicate.addProperty(it.key.toString(), it.value)
                    }

                    eachOverride.add("predicate", eachPredicate)
                    eachOverride.addProperty("model", override.model.toString())

                    overrideJsonArray.add(eachOverride)
                }
                jsonObject.add("overrides", overrideJsonArray)
            }

            jsonObject
        })

        return builder.model.id
    }

    fun generate(builder: Builder) {
        val modelCollector = generator.writer

        modelCollector.accept(builder)

        builder.overrides.forEach { override ->
            modelCollector.accept(
                builder {
                    model (override.model.id.path) {
                        parent { override.model.parent }
                        textures {
                            from (override.model.textures) { it }
                        }
                    }
                }
            )
        }
    }
}
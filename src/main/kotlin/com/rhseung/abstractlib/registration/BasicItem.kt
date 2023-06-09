package com.rhseung.abstractlib.registration

import com.rhseung.abstractlib.api.file.Location
import com.rhseung.abstractlib.api.StringStyle.titlecase
import com.rhseung.abstractlib.api.annotation.en_us
import net.minecraft.item.Item
import kotlin.reflect.KClass

open class BasicItem(
    val id: Location,
    private val setting: Settings
) : Item(setting), IBasicRegistryKey {
    constructor(loc: Location) : this(loc, Settings())
    
    override var translationName = mutableMapOf<KClass<*>, String>(
        en_us::class to id.path.titlecase()
    )

    override fun toString(): String {
        return "BasicItem(loc=$id, setting=$setting)"
    }
}
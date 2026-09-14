package com.dmytrosamoilov.offhand.core.data.domain

sealed interface NoteStyleRef {

    data class BuiltIn(val preset: NotePreset) : NoteStyleRef

    data class Custom(val id: Long) : NoteStyleRef

    fun storageKey(): String = when (this) {
        is BuiltIn -> preset.name
        is Custom -> "$CUSTOM_PREFIX$id"
    }

    companion object {
        val DEFAULT: NoteStyleRef = BuiltIn(NotePreset.DEFAULT)
        private const val CUSTOM_PREFIX = "custom:"

        fun fromStorageKey(key: String?): NoteStyleRef {
            val customId = key?.removePrefix(CUSTOM_PREFIX)?.takeIf { it != key }?.toLongOrNull()
            return if (customId != null) Custom(customId) else BuiltIn(NotePreset.fromName(key))
        }
    }
}

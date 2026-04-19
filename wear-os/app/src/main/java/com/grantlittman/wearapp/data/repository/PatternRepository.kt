package com.grantlittman.wearapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.grantlittman.wearapp.data.DefaultPresets
import com.grantlittman.wearapp.data.model.Pattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/** DataStore instance scoped to the application context. */
private val Context.dataStore by preferencesDataStore(name = "pulsetimer_patterns")

/**
 * Repository that persists patterns using Jetpack DataStore + Gson.
 * Simple and reliable — no annotation processing needed.
 */
class PatternRepository(private val context: Context) {

    private val gson = Gson()
    private val patternsKey = stringPreferencesKey("patterns_json")

    /**
     * All patterns as a reactive Flow, sorted with presets first then by recency.
     */
    /**
     * All patterns as a reactive Flow. Sorting is handled by the UI layer.
     */
    val allPatterns: Flow<List<Pattern>> = context.dataStore.data.map { prefs ->
        deserialize(prefs[patternsKey])
    }

    /**
     * Initialize the store with default presets if empty.
     * Call this once at app startup.
     */
    suspend fun seedPresetsIfNeeded() {
        context.dataStore.edit { prefs ->
            val existing = deserialize(prefs[patternsKey])
            if (existing.none { it.isPreset }) {
                val seeded = existing + DefaultPresets.all
                prefs[patternsKey] = serialize(seeded)
            }
        }
    }

    suspend fun getById(id: String): Pattern? {
        val prefs = context.dataStore.data.first()
        return deserialize(prefs[patternsKey]).find { it.id == id }
    }

    suspend fun save(pattern: Pattern) {
        context.dataStore.edit { prefs ->
            val patterns = deserialize(prefs[patternsKey]).toMutableList()
            // Replace if exists, otherwise add
            val index = patterns.indexOfFirst { it.id == pattern.id }
            if (index >= 0) {
                patterns[index] = pattern
            } else {
                patterns.add(pattern)
            }
            prefs[patternsKey] = serialize(patterns)
        }
    }

    suspend fun delete(pattern: Pattern) {
        if (pattern.isPreset) return // Safety: never delete presets
        context.dataStore.edit { prefs ->
            val patterns = deserialize(prefs[patternsKey]).toMutableList()
            patterns.removeAll { it.id == pattern.id }
            prefs[patternsKey] = serialize(patterns)
        }
    }

    suspend fun duplicate(pattern: Pattern, newName: String): Pattern {
        val copy = pattern.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            isPreset = false,
            lastUsedAt = null
        )
        save(copy)
        return copy
    }

    suspend fun markUsed(id: String) {
        context.dataStore.edit { prefs ->
            val patterns = deserialize(prefs[patternsKey]).toMutableList()
            val index = patterns.indexOfFirst { it.id == id }
            if (index >= 0) {
                patterns[index] = patterns[index].copy(lastUsedAt = System.currentTimeMillis())
                prefs[patternsKey] = serialize(patterns)
            }
        }
    }

    // -- Serialization helpers --

    private fun serialize(patterns: List<Pattern>): String = gson.toJson(patterns)

    private fun deserialize(json: String?): List<Pattern> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<Pattern>>() {}.type
            gson.fromJson<List<Pattern>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            // Corrupted or incompatible JSON — return empty rather than crash
            emptyList()
        }
    }
}

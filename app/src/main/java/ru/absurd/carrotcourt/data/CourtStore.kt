package ru.absurd.carrotcourt.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CourtStore(context: Context) {
    private val preferences = context.getSharedPreferences("carrot_court_v1", Context.MODE_PRIVATE)
    fun read(): CourtState = runCatching {
        val raw = preferences.getString("state", null) ?: return CourtState()
        val json = JSONObject(raw)
        val found = json.optJSONArray("found") ?: JSONArray()
        val records = json.optJSONArray("history") ?: JSONArray()
        CourtState(
            dialogue = Dialogue(json.getString("speaker"), json.getString("text")),
            closed = json.optBoolean("closed"), completed = json.optInt("completed"),
            guilty = json.optInt("guilty"), acquitted = json.optInt("acquitted"),
            sound = json.optBoolean("sound", true),
            found = (0 until found.length()).map { found.getString(it) }.toSet(),
            history = (0 until records.length()).map { i -> records.getJSONObject(i).let {
                CaseRecord(it.getInt("number"), it.getString("text"), it.getBoolean("guilty"), it.getBoolean("secret"))
            } },
            event = AbsurdCatalog.events.find { it.id == json.optString("event") }
        )
    }.getOrDefault(CourtState())

    fun write(state: CourtState) {
        val records = JSONArray()
        state.history.forEach { records.put(JSONObject().put("number", it.number).put("text", it.text)
            .put("guilty", it.guilty).put("secret", it.secret)) }
        val json = JSONObject().put("speaker", state.dialogue.speaker).put("text", state.dialogue.text)
            .put("closed", state.closed).put("completed", state.completed).put("guilty", state.guilty)
            .put("acquitted", state.acquitted).put("sound", state.sound).put("found", JSONArray(state.found.toList()))
            .put("history", records).put("event", state.event?.id)
        preferences.edit().putString("state", json.toString()).apply()
    }
}

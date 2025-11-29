package com.example.perspecto.di

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object SupabaseModule {
    private const val SUPABASE_URL = "https://pwvtqemkhxepbdvisvwb.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB3dnRxZW1raHhlcGJkdmlzdndiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMxMTM0NDcsImV4cCI6MjA2ODY4OTQ0N30.f8dQhMPEbsHj8n-2p1pFgqHxdfX8Z-4p7vYo9ekE2lg"

    lateinit var client: SupabaseClient

    fun initialize(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                sessionManager = AndroidSessionManager(context)
            }
            install(Storage)
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })
        }
        println("Supabase Client Initialized with URL: $SUPABASE_URL")
    }
}

class AndroidSessionManager(context: Context) : SessionManager {
    private val sharedPrefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        val json = Json.encodeToString(session)
        sharedPrefs.edit().putString("session", json).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val json = sharedPrefs.getString("session", null) ?: return null
        return try {
            Json.decodeFromString<UserSession>(json)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        sharedPrefs.edit().remove("session").apply()
    }
}

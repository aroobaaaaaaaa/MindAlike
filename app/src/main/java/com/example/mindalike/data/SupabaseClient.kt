package com.example.mindalike.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://pflwjbnjvukkuoqfyznl.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmbHdqYm5qdnVra3VvcWZ5em5sIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk3NTM0MzcsImV4cCI6MjEwNTMyOTQzN30.4rroFRg_blJ23FCg1KwjUyU3emd9Tr5T6BGJWGBIBPE"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}
package com.example.triade_monitoramento.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://dmfnfiklehsgtrlcmyhl.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRtZm5maWtsZWhzZ3RybGNteWhsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzA2NDE5MjksImV4cCI6MjA4NjIxNzkyOX0.60pjZ6FvJXf40J5_hyNbAJ9W8wOot4JWL97353-6ZRI"
    ) {
        install(Auth.Companion)
        install(Postgrest.Companion)
    }
}
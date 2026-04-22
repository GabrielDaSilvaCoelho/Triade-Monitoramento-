package com.example.triade_monitoramento

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

class UsuarioRepository {
    private val client = SupabaseClientProvider.client

    suspend fun cadastrar(
        nome: String,
        email: String,
        cpf: String,
        telefone: String?,
        senha: String
    ): UsuarioRow {

        val result = client
            .from("usuario")
            .insert(
                mapOf(
                    "nome" to nome,
                    "cpf" to cpf,
                    "email" to email,
                    "telefone" to telefone,
                    "senha_hash" to senha
                )
            )
            .decodeSingle<UsuarioRow>()

        return result
    }

    suspend fun login(email: String, senha: String): UsuarioRow? {
        val result = client.postgrest.rpc(
            function = "login_usuario",
            parameters = mapOf(
                "p_email" to email,
                "p_senha" to senha
            )
        )
        return result.decodeList<UsuarioRow>().firstOrNull()
    }

    suspend fun buscarUsuarioLogado(): UsuarioRow? {
        val userId = Session.userId ?: return null

        val result = client
            .from("usuario")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeList<UsuarioRow>()

        return result.firstOrNull()
    }
}
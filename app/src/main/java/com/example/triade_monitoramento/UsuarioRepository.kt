package com.example.triade_monitoramento

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

class UsuarioRepository {
    private val client = SupabaseClientProvider.client



    suspend fun cadastrar(nome: String, email: String, cpf: String, telefone: String?, senha: String): UsuarioRow {
        val result = client.postgrest.rpc(
            function = "register_usuario",
            parameters = mapOf(
                "p_nome" to nome,
                "p_cpf" to cpf,
                "p_email" to "",
                "p_telefone" to (telefone ?: ""),
                "p_senha" to senha
            )
        )
        return result.decodeSingle()
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
}
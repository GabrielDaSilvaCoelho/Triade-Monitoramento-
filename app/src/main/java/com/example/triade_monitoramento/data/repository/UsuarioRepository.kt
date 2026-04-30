package com.example.triade_monitoramento.data.repository

import android.util.Log
import com.example.triade_monitoramento.Session
import com.example.triade_monitoramento.data.remote.SupabaseClientProvider
import com.example.triade_monitoramento.ui.components.UsuarioRow
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import java.time.LocalDateTime

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

    suspend fun atualizarPerfil(
        nome: String,
        email: String,
        telefone: String
    ): Boolean {
        return try {
            val userId = Session.userId

            Log.d("PERFIL_DEBUG", "userId atual: $userId")
            Log.d("PERFIL_DEBUG", "nome=$nome email=$email telefone=$telefone")

            if (userId == null) {
                Log.e("PERFIL_DEBUG", "Session.userId está nulo")
                return false
            }

            client
                .from("usuario")
                .update(
                    {
                        set("nome", nome)
                        set("email", email)
                        set("telefone", telefone)
                    }
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d("PERFIL_DEBUG", "Perfil atualizado com sucesso no Supabase")
            true

        } catch (e: Exception) {
            Log.e("ERRO_USUARIO", "Erro ao atualizar perfil", e)
            false
        }
    }

    suspend fun solicitarRecuperacaoSenha(email: String): Boolean {
        return try {
            val codigo = (100000..999999).random().toString()
            val expiraEm = LocalDateTime.now().plusMinutes(10).toString()

            client
                .from("usuario")
                .update(
                    {
                        set("reset_code", codigo)
                        set("reset_expires_at", expiraEm)
                    }
                ) {
                    filter {
                        eq("email", email)
                    }
                }

            Log.d("RESET_SENHA", "Código de recuperação para $email: $codigo")

            true
        } catch (e: Exception) {
            Log.e("RESET_SENHA", "Erro ao solicitar recuperação de senha", e)
            false
        }
    }

    suspend fun redefinirSenha(
        email: String,
        codigo: String,
        novaSenha: String
    ): Boolean {
        return try {
            val result = client.postgrest.rpc(
                function = "redefinir_senha_segura",
                parameters = mapOf(
                    "p_email" to email,
                    "p_codigo" to codigo,
                    "p_nova_senha" to novaSenha
                )
            )

            result.decodeAs<Boolean>()
        } catch (e: Exception) {
            Log.e("RESET_SENHA", "Erro ao redefinir senha", e)
            false
        }
    }
}

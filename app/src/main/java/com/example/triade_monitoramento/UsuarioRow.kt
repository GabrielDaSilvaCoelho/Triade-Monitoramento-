package com.example.triade_monitoramento

import kotlinx.serialization.Serializable

@Serializable
data class UsuarioRow(
    val id: Int,
    val email: String?,
    val nome: String,
    val cpf: String,
    val telefone: String?
)


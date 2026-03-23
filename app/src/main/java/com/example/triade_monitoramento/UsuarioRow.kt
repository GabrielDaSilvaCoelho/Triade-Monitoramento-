package com.example.triade_monitoramento

import kotlinx.serialization.Serializable

@Serializable
data class UsuarioRow(
    val id: String? = null,
    val email: String? = null,
    val nome: String,
    val cpf: String,
    val telefone: String? = null
)


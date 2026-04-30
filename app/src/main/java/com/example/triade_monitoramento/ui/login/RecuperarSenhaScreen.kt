package com.example.triade_monitoramento.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

@Composable
fun RecuperarSenhaScreen(
    repo: UsuarioRepository = remember { UsuarioRepository() },
    onVoltarLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }

    var etapa by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val triadeGreen = Color(0xFF769F86)
    val corBotao = Color(0xFF293944)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Recuperar senha",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF1F1F1F)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (etapa == 1) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Digite seu email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = triadeGreen,
                    unfocusedBorderColor = triadeGreen,
                    cursorColor = triadeGreen
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    msg = null

                    if (email.isBlank()) {
                        msg = "Digite seu email."
                        return@Button
                    }

                    loading = true

                    scope.launch {
                        val sucesso = repo.solicitarRecuperacaoSenha(email.trim())

                        if (sucesso) {
                            msg = "Código gerado. Veja o Logcat em RESET_SENHA."
                            etapa = 2
                        } else {
                            msg = "Não foi possível gerar o código."
                        }

                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corBotao)
            ) {
                Text(if (loading) "Enviando..." else "Gerar código", color = Color.White)
            }
        } else {
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it },
                label = { Text("Código de recuperação") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = novaSenha,
                onValueChange = { novaSenha = it },
                label = { Text("Nova senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmarSenha,
                onValueChange = { confirmarSenha = it },
                label = { Text("Confirmar nova senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    msg = null

                    if (codigo.isBlank() || novaSenha.isBlank() || confirmarSenha.isBlank()) {
                        msg = "Preencha todos os campos."
                        return@Button
                    }

                    if (novaSenha != confirmarSenha) {
                        msg = "As senhas não são iguais."
                        return@Button
                    }

                    loading = true

                    scope.launch {
                        val sucesso = repo.redefinirSenha(
                            email = email.trim(),
                            codigo = codigo.trim(),
                            novaSenha = novaSenha
                        )

                        if (sucesso) {
                            msg = "Senha alterada com sucesso."
                            onVoltarLogin()
                        } else {
                            msg = "Código inválido ou expirado."
                        }

                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corBotao)
            ) {
                Text(if (loading) "Salvando..." else "Alterar senha", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onVoltarLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Voltar para login")
        }

        msg?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = Color.DarkGray)
        }
    }
}
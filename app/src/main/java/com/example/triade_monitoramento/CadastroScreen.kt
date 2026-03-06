package com.example.triade_monitoramento

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CadastroScreen(
    repo: UsuarioRepository = remember { UsuarioRepository() },
    onCadastrado: (UsuarioRow) -> Unit,
    onIrParaLogin: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),

        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo do app",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Cadastro",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome", color = Color(0xFF769F86)) },
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF769F86)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

            shape = RoundedCornerShape(8.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF769F86),
                unfocusedBorderColor = Color(0xFF769F86),
                focusedLabelColor = Color(0xFF769F86),
                cursorColor = Color(0xFF769F86)
            ),

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = cpf,
            onValueChange = { cpf = it },
            label = { Text("Cpf", color = Color(0xFFC9BF5A)) },
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFFC9BF5A)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

            shape = RoundedCornerShape(8.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFC9BF5A),
                unfocusedBorderColor = Color(0xFFC9BF5A),
                focusedLabelColor = Color(0xFFC9BF5A),
                cursorColor = Color(0xFFC9BF5A)
            ),

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = telefone,
            onValueChange = { nome = it },
            label = { Text("telefone(opcional)", color = Color(0xFF769F86)) },
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF769F86)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

            shape = RoundedCornerShape(8.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF769F86),
                unfocusedBorderColor = Color(0xFF769F86),
                focusedLabelColor = Color(0xFF769F86),
                cursorColor = Color(0xFF769F86)
            ),

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha", color = Color(0xFFC9BF5A)) },
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFFC9BF5A)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFC9BF5A),
                unfocusedBorderColor = Color(0xFFC9BF5A),
                focusedLabelColor = Color(0xFFC9BF5A),
                cursorColor = Color(0xFFC9BF5A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                msg = null

                if (nome.isBlank() || cpf.isBlank() || senha.isBlank()) {
                    msg = "Preencha nome, CPF e senha"
                    return@Button
                }

                loading = true

                scope.launch {
                    try {
                        val user = repo.cadastrar(
                            nome = nome.trim(),
                            cpf = cpf.trim(),
                            telefone = telefone.trim().ifBlank { null },
                            senha = senha
                        )

                        onCadastrado(user)

                    } catch (e: Exception) {
                        msg = "Erro: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Cadastrando..." else "Cadastrar")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onIrParaLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Já possui conta? Ir para login",
                color = Color(0xFFFF0000)
            )
        }

        msg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
    }
}
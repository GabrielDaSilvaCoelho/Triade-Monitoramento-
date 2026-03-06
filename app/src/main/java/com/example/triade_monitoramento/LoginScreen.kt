package com.example.triade_monitoramento

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    background: Color = Color.White,
    repo: UsuarioRepository = remember { UsuarioRepository() },
    onLogado: (UsuarioRow) -> Unit,
    onIrParaCadastro: () -> Unit
) {
    var cpf by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val corBotao = Color(0xFF293944)
    val corTextoCriar = Color(0xFFD32F2F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(70.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo do app",
            modifier = Modifier
                .size(200.dp)
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = cpf,
            onValueChange = { cpf = it },
            label = { Text("Insira seu CPF", color = Color(0xFF769F86)) },
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

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Insira sua senha", color = Color(0xFFC9BF5A)) },
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFFC9BF5A)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),

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

        Spacer(Modifier.height(24.dp))

        Button(
            shape = RoundedCornerShape(8.dp),
            onClick = {
                msg = null
                if (cpf.isBlank() || senha.isBlank()) {
                    msg = "Preencha CPF e senha."
                    return@Button
                }

                loading = true
                scope.launch {
                    try {
                        val user = repo.login(cpf.trim(), senha)

                        if (user != null) {
                            msg = "Login realizado com sucesso"
                            onLogado(user)
                        } else {
                            msg = "CPF ou senha inválidos"
                        }
                    } catch (e: Exception) {
                        msg = "Erro: ${e.localizedMessage}"
                        e.printStackTrace()
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = corBotao),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
                .height(52.dp)

        ) {
            Text(text = if (loading) "Entrando" else "Entrar", color = Color.White)
        }
        Spacer(Modifier.height(24.dp))

        val context = LocalContext.current

        TextButton(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://html-final-zeta.vercel.app/")
                )
                context.startActivity(intent)
            }
        ) {
            Text(
                text = "Não possui conta? Criar conta",
                color = Color(0xFFFF0000)
            )
        }

        msg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
    }
}

package com.example.triade_monitoramento.ui.perfil

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val TriadeGreen = Color(0xFF769F86)
private val TriadeBorder = Color(0xFF8AA796)
private val BackgroundColor = Color(0xFFF7F9F8)
private val TextDark = Color(0xFF1F1F1F)

data class UsuarioPerfil(
    val nome: String,
    val email: String,
    val telefone: String
)

@Composable
fun PerfilScreen(
    usuario: UsuarioPerfil,
    onVoltar: () -> Unit = {},
    onSalvarPerfil: (UsuarioPerfil) -> Unit = {}
) {
    var editando by remember { mutableStateOf(false) }

    var nome by remember { mutableStateOf(usuario.nome) }
    var email by remember { mutableStateOf(usuario.email) }
    var telefone by remember { mutableStateOf(usuario.telefone) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    CampoPerfilEditavel(
                        titulo = "Nome",
                        valor = nome,
                        editando = editando,
                        icone = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TriadeGreen)
                        },
                        onValorChange = { nome = it }
                    )

                    CampoPerfilEditavel(
                        titulo = "Gmail",
                        valor = email,
                        editando = editando,
                        icone = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TriadeGreen)
                        },
                        onValorChange = { email = it }
                    )

                    CampoPerfilEditavel(
                        titulo = "Telefone",
                        valor = telefone,
                        editando = editando,
                        icone = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TriadeGreen)
                        },
                        onValorChange = { telefone = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (editando) {
                        onSalvarPerfil(
                            UsuarioPerfil(nome, email, telefone)
                        )
                    }
                    editando = !editando
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TriadeGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (editando) "Salvar alterações" else "Editar perfil"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onVoltar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text("Voltar")
            }
        }
    }
}

@Composable
fun CampoPerfilEditavel(
    titulo: String,
    valor: String,
    editando: Boolean,
    icone: @Composable () -> Unit,
    onValorChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = TriadeBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.padding(bottom = 8.dp)) {
            icone()
        }

        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (editando) {
            OutlinedTextField(
                value = valor,
                onValueChange = onValorChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        } else {
            Text(
                text = valor,
                style = MaterialTheme.typography.bodyLarge,
                color = TextDark,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
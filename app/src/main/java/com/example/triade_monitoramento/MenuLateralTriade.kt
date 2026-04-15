package com.example.triade_monitoramento.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.R
import kotlinx.coroutines.launch

private val TriadeGreen = Color(0xFF769F86)
private val TriadeGreenLight = Color(0xFFDDE9E1)
private val TriadeBorder = Color(0xFF8AA796)
private val DrawerWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF1F1F1F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuLateralTriade(
    onCadastrarSensor: () -> Unit,
    onPerfil: () -> Unit,
    onSensores: () -> Unit,
    onSair: () -> Unit,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight(),
                drawerContainerColor = Color.Transparent
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 10.dp, top = 12.dp, bottom = 12.dp, end = 24.dp),
                    color = DrawerWhite,
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = TriadeBorder,
                                    shape = CircleShape
                                )
                                .background(TriadeGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = "Logo da Triade",
                                modifier = Modifier.size(74.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Monitoramento",
                            color = TextDark,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        MenuLateralBotao(
                            texto = "Cadastrar Sensor",
                            destaque = true,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onCadastrarSensor()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MenuLateralBotao(
                            texto = "Perfil",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onPerfil()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MenuLateralBotao(
                            texto = "Sensores",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onSensores()
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        MenuLateralBotao(
                            texto = "Sair",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onSair()
                            }
                        )
                    }
                }
            }
        }
    ) {
        val openDrawer: () -> Unit = {
            scope.launch {
                drawerState.open()
            }
        }

        content(openDrawer)
    }
}

@Composable
fun MenuLateralBotao(
    texto: String,
    destaque: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destaque) TriadeGreen else Color.White,
            contentColor = if (destaque) Color.White else TriadeGreen
        ),
        border = if (destaque) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(1.5.dp, TriadeBorder)
        },
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Text(
            text = texto,
            fontWeight = FontWeight.Medium
        )
    }
}
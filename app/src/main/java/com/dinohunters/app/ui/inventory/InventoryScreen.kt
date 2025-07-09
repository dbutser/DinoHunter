// com.dinohunters.app.ui.inventory.InventoryScreen.kt
package com.dinohunters.app.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dinohunters.app.data.model.BoneKey // Импортируем BoneKey
import com.dinohunters.app.data.model.BoneRarity // Импортируем BoneRarity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Инвентарь") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Отображение состояния загрузки, ошибок и пустого инвентаря
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.errorMessage ?: "Неизвестная ошибка загрузки инвентаря", color = MaterialTheme.colorScheme.error)
            }
        } else if (uiState.groupedBones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Инвентарь пуст. Время на охоту!")
            }
        } else {
            // Если есть сгруппированные кости, отображаем их
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Используем .entries.toList() для итерации по парам ключ-значение
                items(uiState.groupedBones.entries.toList()) { (boneKey, count) ->
                    // Передаем BoneKey и количество в новую композабл функцию
                    GroupedBoneCard(boneKey = boneKey, count = count)
                }
            }
        }
    }
}

// Новая композабл функция для отображения сгруппированной карточки кости
@Composable
fun GroupedBoneCard(boneKey: BoneKey, count: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = boneKey.imageUrl, // Используем imageUrl из BoneKey
                contentDescription = boneKey.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(boneKey.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Тип: ${boneKey.type}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                // Карточка редкости
                Card(
                    // Используем Color(Long) напрямую, так как boneKey.rarity.color уже Long
                    colors = CardDefaults.cardColors(containerColor = Color(boneKey.rarity.color))
                ) {
                    Text(
                        text = boneKey.rarity.displayName,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Отображаем количество, если оно больше 1
            if (count > 1) {
                Text(
                    text = "x$count",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // Или любой другой цвет
                )
            }
        }
    }
}
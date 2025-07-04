// Путь: app/src/main/java/com/dinohunters/app/ui/profile/ProfileScreen.kt

package com.dinohunters.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinohunters.app.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // [КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ]
    // Используем collectAsStateWithLifecycle для надежной и эффективной подписки на обновления.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")                    }
                }
            )
        }
    ) { paddingValues ->
        // Проверяем, загружен ли профиль, чтобы избежать показа данных по умолчанию (все по нулям)
        if (uiState.isLoading) {
            // Если в ViewModel стоит флаг isLoading, показываем индикатор
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Когда данные готовы, показываем контент
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileHeader(profile = uiState.profile)
                TotalBonesStatistic(profile = uiState.profile)
                RarityStatistics(profile = uiState.profile)
            }
        }
    }
}

@Composable
fun ProfileHeader(profile: UserProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Аватар", modifier = Modifier.size(64.dp))
            Text(profile.nickname, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            // Добавим проверку, чтобы не было креша, если createdAt == 0
            if (profile.createdAt > 0) {
                Text(
                    "Охотник с ${SimpleDateFormat("dd MMMM yyyy", Locale("ru")).format(Date(profile.createdAt))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun TotalBonesStatistic(profile: UserProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Всего найдено костей:", style = MaterialTheme.typography.titleMedium)
            Text(
                text = profile.totalBones.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RarityStatistics(profile: UserProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Коллекция по редкости", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Создаем список данных для отображения, чтобы было чисто и наглядно
            val stats = listOf(
                "Обычные" to profile.commonBones,
                "Необычные" to profile.uncommonBones,
                "Редкие" to profile.rareBones,
                "Эпические" to profile.epicBones,
                "Легендарные" to profile.legendaryBones
            )

            stats.forEach { (name, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Text(count.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                if (stats.last().first != name) {
                    Divider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
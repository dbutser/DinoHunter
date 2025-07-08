package com.dinohunters.app.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinohunters.app.R
import com.dinohunters.app.data.model.UserProfile
import com.dinohunters.app.service.StepCounter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val stepCounter = StepCounter(context)
    val hasStepSensor = stepCounter.hasStepCounterSensor()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(key1 = hasStepSensor) {
        if (hasStepSensor) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !stepCounter.hasPermission()) {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                viewModel.syncDinoCoins()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Аватар", modifier = Modifier.size(56.dp))
                Column {
                    Text(profile.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (profile.createdAt > 0) {
                        Text(
                            "Сыщикозавр",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dinocoin),
                    contentDescription = "Динокоины",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = profile.dinocoinBalance.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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

            val stats = listOf(
                "Обычные" to profile.commonBones,
                "Необычные" to profile.uncommonBones,
                "Редкие" to profile.rareBones,
                "Эпические" to profile.epicBones,
                "Легендарные" to profile.legendaryBones
            )

            stats.forEach { (name, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
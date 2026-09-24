package com.moneyprinter.turbo.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.moneyprinter.turbo.data.local.AppDatabase
import com.moneyprinter.turbo.data.model.TaskEntity
import com.moneyprinter.turbo.data.model.TaskState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).taskDao() }
    val tasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Riwayat Task",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada task.\nBuat video dulu di tab Buat.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        dateFormat = dateFormat,
                        onPlay = {
                            task.finalVideoPath?.let { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "video/mp4")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // fallback open with path
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.fromFile(file), "video/mp4")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        },
                        onDelete = {
                            scope.launch { dao.delete(task.id) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    dateFormat: SimpleDateFormat,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val stateColor = when (task.state) {
        TaskState.COMPLETE.name -> MaterialTheme.colorScheme.primary
        TaskState.FAILED.name -> MaterialTheme.colorScheme.error
        TaskState.PROCESSING.name -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    task.videoSubject.ifBlank { "(tanpa topik)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    task.state,
                    style = MaterialTheme.typography.labelMedium,
                    color = stateColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                dateFormat.format(Date(task.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!task.error.isNullOrBlank()) {
                Text(
                    "Error: ${task.error}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.state == TaskState.COMPLETE.name && !task.finalVideoPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Putar")
                    }
                    OutlinedButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Hapus")
                    }
                }
            } else if (task.state == TaskState.FAILED.name) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus")
                }
            }
        }
    }
}

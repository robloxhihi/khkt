package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmotionTag
import com.example.data.model.FamilyMessage
import com.example.data.model.UserRole
import com.example.ui.components.EmotionSelectorRow
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceCardElevated
import com.example.ui.theme.SpaceCardSurface
import com.example.ui.theme.SpaceDarkBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.VesperaUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    uiState: VesperaUiState,
    onSendMessage: (String) -> Unit,
    onToggleStar: (String, Boolean) -> Unit,
    onSelectEmotion: (EmotionTag) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var filterStarredOnly by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val currentUserId = uiState.userProfile?.id.orEmpty()
    val partnerName = if (uiState.userProfile?.role == UserRole.CHILD) {
        uiState.activePairing?.parentName.orEmpty().ifEmpty { "Phụ huynh" }
    } else {
        uiState.activePairing?.childName.orEmpty().ifEmpty { "Học sinh" }
    }

    val displayMessages = if (filterStarredOnly) {
        uiState.messages.filter { it.isStarred }
    } else {
        uiState.messages
    }

    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    val quickPresets = listOf(
        "Con đã về đến nhà an toàn!",
        "Bố mẹ sắp qua đón con nhé",
        "Con xin phép học bài thêm 30 phút",
        "Nhớ uống nước và nghỉ ngơi nhé ❤️",
        "Con đang ở trường học"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBackground)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GIA ĐÌNH: $partnerName",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Đồng bộ realtime trên Cloud Firestore",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonEmerald
                )
            }

            // Star Filter Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (filterStarredOnly) NeonAmber.copy(alpha = 0.2f) else SpaceCardElevated)
                    .border(1.dp, if (filterStarredOnly) NeonAmber else SpaceCardBorder, RoundedCornerShape(12.dp))
                    .clickable { filterStarredOnly = !filterStarredOnly }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (filterStarredOnly) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Starred filter",
                        tint = if (filterStarredOnly) NeonAmber else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tin đã lưu sao",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (filterStarredOnly) NeonAmber else TextSecondary
                    )
                }
            }
        }

        // Quick Presets Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPresets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpaceCardElevated)
                        .border(1.dp, SpaceCardBorder, RoundedCornerShape(16.dp))
                        .clickable { onSendMessage(preset) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (displayMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filterStarredOnly) "Chưa có tin nhắn nào được lưu sao." else "Bắt đầu cuộc trò chuyện gia đình!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            items(displayMessages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUserId
                MessageBubble(
                    message = msg,
                    isMe = isMe,
                    onToggleStar = { onToggleStar(msg.id, !msg.isStarred) }
                )
            }
        }

        // Emotion selector bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            EmotionSelectorRow(
                selectedEmotion = uiState.selectedEmotion,
                onSelectEmotion = onSelectEmotion
            )
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceCardSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Nhập tin nhắn cho gia đình...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = SpaceCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (messageText.isNotBlank() || uiState.selectedEmotion != null) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
                    .testTag("send_chat_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Gửi",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: FamilyMessage,
    isMe: Boolean,
    onToggleStar: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.createdAt))

    val emotionEmoji = when (message.emotion) {
        EmotionTag.HAPPY.name -> "😊"
        EmotionTag.NORMAL.name -> "😐"
        EmotionTag.STUDY_STRESS.name -> "📚"
        EmotionTag.NEED_SUPPORT.name -> "🆘"
        else -> null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) NeonViolet.copy(alpha = 0.35f) else SpaceCardElevated)
                .border(
                    1.dp,
                    if (isMe) NeonViolet.copy(alpha = 0.6f) else SpaceCardBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isMe) "Bạn" else message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) NeonCyan else NeonViolet,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (message.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star message",
                            tint = if (message.isStarred) NeonAmber else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (emotionEmoji != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Tâm trạng: $emotionEmoji", fontSize = 16.sp)
                }

                if (message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

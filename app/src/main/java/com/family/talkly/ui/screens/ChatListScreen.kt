package com.family.talkly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.family.talkly.ui.theme.LocalIsDarkTheme
import com.family.talkly.ui.theme.ThemeMode
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal
import com.family.talkly.data.models.CallType
import com.family.talkly.data.models.ChatMessage
import com.family.talkly.data.models.DEFAULT_FAMILY_MEMBERS
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.data.models.UserProfile
import com.family.talkly.ui.components.ContactProfileDetailsDialog
import com.family.talkly.ui.components.UserProfileDetailsDialog
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal

import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FloatingActionButton
import com.family.talkly.ui.components.AddContactDialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    familyMembers: List<FamilyMember>,
    messagesMap: Map<String, List<ChatMessage>>,
    simulatedTimeOffsetMs: Long,
    currentUserProfile: UserProfile? = null,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: ((ThemeMode) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onSaveProfile: ((name: String, bio: String, photoUrl: String) -> Unit)? = null,
    onSelectMember: (FamilyMember) -> Unit,
    onStartCall: (FamilyMember, CallType) -> Unit,
    onTriggerIncomingDemo: (FamilyMember) -> Unit,
    onTogglePinMember: ((String) -> Unit)? = null,
    onSearchUserByPhone: ((phone: String, onResult: (UserProfile?) -> Unit) -> Unit)? = null,
    onAddContact: ((name: String, phone: String, relation: String, bio: String, avatarUrl: String?) -> Unit)? = null,
    onDeleteContact: ((String) -> Unit)? = null,
    onDeleteChatHistory: ((String) -> Unit)? = null,
    onClearDemoContacts: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var selectedContactForProfile by remember { mutableStateOf<FamilyMember?>(null) }
    var memberToDeleteHistory by remember { mutableStateOf<FamilyMember?>(null) }

    val isDark = LocalIsDarkTheme.current

    if (showAddContactDialog && onSearchUserByPhone != null && onAddContact != null) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onSearchUserByPhone = { phone, callback ->
                onSearchUserByPhone(phone, callback)
            },
            onAddContact = { name, phone, relation, bio, avatarUrl ->
                onAddContact(name, phone, relation, bio, avatarUrl)
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = WhatsappTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose App Theme", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    val options = listOf(
                        Triple(ThemeMode.LIGHT, "Light Mode ☀️", "Bright visual theme"),
                        Triple(ThemeMode.DARK, "Dark Mode 🌙", "Eye-safe dark canvas"),
                        Triple(ThemeMode.SYSTEM, "System Default 📱", "Match device system setting")
                    )
                    options.forEach { (mode, label, subtitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange?.invoke(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentThemeMode == mode),
                                onClick = {
                                    onThemeModeChange?.invoke(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = WhatsappTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = label,
                                    fontWeight = if (currentThemeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = WhatsappTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showProfileDialog && currentUserProfile != null) {
        UserProfileDetailsDialog(
            userProfile = currentUserProfile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, bio, photoUrl ->
                onSaveProfile?.invoke(name, bio, photoUrl)
            },
            onLogout = onLogout
        )
    }

    if (selectedContactForProfile != null) {
        ContactProfileDetailsDialog(
            member = selectedContactForProfile!!,
            onDismiss = { selectedContactForProfile = null },
            onStartChat = { member ->
                selectedContactForProfile = null
                onSelectMember(member)
            },
            onStartCall = { member, callType ->
                selectedContactForProfile = null
                onStartCall(member, callType)
            },
            onDeleteContact = { id ->
                selectedContactForProfile = null
                onDeleteContact?.invoke(id)
            }
        )
    }

    if (memberToDeleteHistory != null) {
        AlertDialog(
            onDismissRequest = { memberToDeleteHistory = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
            },
            title = {
                Text(
                    text = "Delete Chat History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all chat history with ${memberToDeleteHistory?.name}? This action cannot be undone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = memberToDeleteHistory?.id
                        if (targetId != null) {
                            onDeleteChatHistory?.invoke(targetId)
                            Toast.makeText(context, "Chat history deleted", Toast.LENGTH_SHORT).show()
                        }
                        memberToDeleteHistory = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDeleteHistory = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Talkly",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Family Call",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val newMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                            onThemeModeChange?.invoke(newMode)
                        }
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = Color.White
                        )
                    }

                    if (currentUserProfile != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { showProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUserProfile.profilePicUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentUserProfile.profilePicUrl,
                                    contentDescription = "My Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = currentUserProfile.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add New Contact") },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = WhatsappGreen)
                            },
                            onClick = {
                                showMenu = false
                                showAddContactDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Demo Contacts") },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray)
                            },
                            onClick = {
                                showMenu = false
                                onClearDemoContacts?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("App Theme (${currentThemeMode.name})") },
                            leadingIcon = {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = WhatsappTeal)
                            },
                            onClick = {
                                showMenu = false
                                showThemeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("My Profile & Phone") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = WhatsappTeal)
                            },
                            onClick = {
                                showMenu = false
                                showProfileDialog = true
                            }
                        )
                        if (onLogout != null) {
                            DropdownMenuItem(
                                text = { Text("Log Out Session", color = Color.Red) },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                                },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhatsappTeal)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = WhatsappGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Contact"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Family Quick Status Stories Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "FAMILY MEMBERS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(familyMembers) { member ->
                        FamilyMemberAvatarStory(
                            member = member,
                            onClick = { selectedContactForProfile = member },
                            onLongClick = { onTriggerIncomingDemo(member) }
                        )
                    }
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f))

            // Family Conversations List
            val sortedMembers = remember(familyMembers) {
                familyMembers.sortedWith(compareByDescending { it.isPinned })
            }
            val activeChatMembers = remember(sortedMembers, messagesMap) {
                sortedMembers.filter { member ->
                    (messagesMap[member.id] ?: emptyList()).isNotEmpty()
                }
            }

            if (activeChatMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active chats yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Saved contacts appear in the Contacts tab. Tap any contact there to start a chat!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(activeChatMembers) { member ->
                        val memberMessages = messagesMap[member.id] ?: emptyList()
                        val lastMessage = memberMessages.lastOrNull()

                        FamilyChatRow(
                            member = member,
                            lastMessage = lastMessage,
                            simulatedTimeOffsetMs = simulatedTimeOffsetMs,
                            onClick = { onSelectMember(member) },
                            onLongClick = { memberToDeleteHistory = member },
                            onAvatarClick = { selectedContactForProfile = member },
                            onAudioCall = { onStartCall(member, CallType.AUDIO) },
                            onVideoCall = { onStartCall(member, CallType.VIDEO) }
                        )
                        Divider(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberAvatarStory(
    member: FamilyMember,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(if (member.isOnline) WhatsappGreen else Color.Gray, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(WhatsappTeal),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUrl != null) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = member.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            if (member.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color(0xFF25D366), CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = member.name.split(" ").firstOrNull() ?: member.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun FamilyChatRow(
    member: FamilyMember,
    lastMessage: ChatMessage?,
    simulatedTimeOffsetMs: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with real-time Online/Offline indicator dot
        Box(
            modifier = Modifier
                .size(54.dp)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(WhatsappTeal),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUrl != null) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = member.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            // Online Status Dot Indicator
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (member.isOnline) Color(0xFF25D366) else Color.Gray, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Chat Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (member.isPinned) {
                        Text(text = "📌 ", fontSize = 14.sp)
                    }
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (member.isTyping) "typing..." else (lastMessage?.formattedTime ?: "Now"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (member.isTyping) WhatsappGreen else Color.Gray,
                        fontWeight = if (member.isTyping) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (member.isTyping) {
                    Text(
                        text = "typing...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = WhatsappGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val previewText = when {
                        lastMessage == null -> member.status
                        lastMessage.isMediaExpired(simulatedTimeOffsetMs) -> "⚠️ Media expired after 48h"
                        lastMessage.mediaUrl != null -> "📷 Photo / Media"
                        else -> lastMessage.textContent
                    }

                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (lastMessage?.isMediaExpired(simulatedTimeOffsetMs) == true) Color(0xFF856404) else Color.Gray,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (member.unreadCount > 0 && !member.isTyping) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(WhatsappGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Audio & Video Call Quick Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onAudioCall,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Audio Call",
                    tint = WhatsappTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onVideoCall,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = WhatsappTeal,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

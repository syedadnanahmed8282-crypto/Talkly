package com.family.talkly.data.firebase

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.family.talkly.data.models.ChatMessage
import com.family.talkly.data.models.DEFAULT_FAMILY_MEMBERS
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.data.models.MessageType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirebaseChatRepository(private val context: Context) {

    companion object {
        const val TAG = "Talkly_FirebaseChat"
        const val FIREBASE_PROJECT_ID = "familycallapp-e6b21"
    }

    private var firestore: FirebaseFirestore? = null
    private var membersListener: ListenerRegistration? = null

    // Real-time family members presence and status
    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(DEFAULT_FAMILY_MEMBERS)
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    // Time offset for live testing 48-hour expiration logic
    private val _simulatedTimeOffsetMs = MutableStateFlow(0L)
    val simulatedTimeOffsetMs: StateFlow<Long> = _simulatedTimeOffsetMs.asStateFlow()

    // Message maps by family member id
    private val _messagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messagesMap: StateFlow<Map<String, List<ChatMessage>>> = _messagesMap.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            Log.i(TAG, "Initialized Firebase Firestore for project $FIREBASE_PROJECT_ID")
            setupFirestorePresenceListener()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Firestore init fallback mode: ${e.localizedMessage}")
        }
        seedInitialFamilyChats()
    }

    private fun setupFirestorePresenceListener() {
        try {
            membersListener = firestore?.collection("family_members")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for family_members: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val updatedMembers = _familyMembers.value.map { member ->
                            val doc = snapshot.documents.firstOrNull { it.id == member.id }
                            if (doc != null) {
                                val online = doc.getBoolean("isOnline") ?: member.isOnline
                                val typing = doc.getBoolean("isTyping") ?: member.isTyping
                                val seen = doc.getString("lastSeen") ?: member.lastSeen
                                member.copy(
                                    isOnline = online,
                                    isTyping = typing,
                                    lastSeen = seen
                                )
                            } else {
                                member
                            }
                        }
                        _familyMembers.value = updatedMembers
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set up Firestore snapshot listener: ${e.localizedMessage}")
        }
    }

    fun setMemberTyping(memberId: String, isTyping: Boolean) {
        val currentList = _familyMembers.value.map { member ->
            if (member.id == memberId) {
                member.copy(isTyping = isTyping)
            } else {
                member
            }
        }
        _familyMembers.value = currentList

        try {
            firestore?.collection("family_members")
                ?.document(memberId)
                ?.set(mapOf("isTyping" to isTyping, "isOnline" to true), com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Firestore setTyping error: ${e.localizedMessage}")
        }
    }

    fun setMemberPresence(memberId: String, isOnline: Boolean, lastSeen: String = if (isOnline) "Online" else "Just now") {
        val currentList = _familyMembers.value.map { member ->
            if (member.id == memberId) {
                member.copy(isOnline = isOnline, lastSeen = lastSeen, isTyping = if (!isOnline) false else member.isTyping)
            } else {
                member
            }
        }
        _familyMembers.value = currentList

        try {
            firestore?.collection("family_members")
                ?.document(memberId)
                ?.set(
                    mapOf("isOnline" to isOnline, "lastSeen" to lastSeen, "isTyping" to if (!isOnline) false else false),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        } catch (e: Exception) {
            Log.w(TAG, "Firestore setPresence error: ${e.localizedMessage}")
        }
    }

    fun toggleMemberPresence(memberId: String) {
        val member = _familyMembers.value.firstOrNull { it.id == memberId } ?: return
        val newOnline = !member.isOnline
        setMemberPresence(memberId, newOnline, if (newOnline) "Online" else "Today at 10:15 AM")
    }

    fun triggerSimulatedTypingReply(memberId: String) {
        // Show typing indicator for 2.5 seconds, then send reply
        setMemberTyping(memberId, true)

        mainHandler.postDelayed({
            setMemberTyping(memberId, false)
            val member = _familyMembers.value.firstOrNull { it.id == memberId }
            val memberName = member?.name ?: "Family"
            
            val replyText = when (memberId) {
                "mom" -> "Love you dear! Stay safe ❤️"
                "dad" -> "Got it! Let me know if you need anything 🚗"
                "grandma" -> "God bless you child! 🍪"
                "brother" -> "Haha cool! Catch up later 🎮"
                "sister" -> "Nice! Sending hugs 🎨"
                else -> "Received! Talk soon 💖"
            }

            val replyMsg = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderId = memberId,
                senderName = memberName,
                receiverId = "self",
                messageType = MessageType.TEXT,
                textContent = replyText,
                timestamp = System.currentTimeMillis()
            )

            val currentList = (_messagesMap.value[memberId] ?: emptyList()).toMutableList()
            currentList.add(replyMsg)

            val updatedMap = _messagesMap.value.toMutableMap()
            updatedMap[memberId] = currentList
            _messagesMap.value = updatedMap

        }, 3000)
    }

    private fun seedInitialFamilyChats() {
        val now = System.currentTimeMillis()
        val hourAgo = now - 3600000L
        val dayAgo = now - 86400000L
        val threeDaysAgo = now - (3 * 86400000L) // 72 hours ago (> 48h)

        val momChat = listOf(
            ChatMessage(
                id = "m1",
                senderId = "mom",
                senderName = "Mom ❤️",
                receiverId = "self",
                messageType = MessageType.TEXT,
                textContent = "Hi sweetie! Are you coming home for Sunday dinner? 🍲",
                timestamp = threeDaysAgo - 10000,
                isRead = true
            ),
            ChatMessage(
                id = "m2",
                senderId = "mom",
                senderName = "Mom ❤️",
                receiverId = "self",
                messageType = MessageType.IMAGE,
                textContent = "Family pie recipe from grandma 🥧",
                mediaUrl = "https://images.unsplash.com/photo-1519869325930-281384150729?w=600&auto=format&fit=crop&q=80",
                timestamp = threeDaysAgo, // > 48h ago -> EXPIRED
                isRead = true
            ),
            ChatMessage(
                id = "m3",
                senderId = "self",
                senderName = "You",
                receiverId = "mom",
                messageType = MessageType.TEXT,
                textContent = "Yes Mom! I will be there at 6 PM.",
                timestamp = dayAgo,
                isRead = true
            ),
            ChatMessage(
                id = "m4",
                senderId = "mom",
                senderName = "Mom ❤️",
                receiverId = "self",
                messageType = MessageType.IMAGE,
                textContent = "Look at our garden flowers today! 🌸",
                mediaUrl = "https://images.unsplash.com/photo-1508615039623-a25605d2b022?w=600&auto=format&fit=crop&q=80",
                timestamp = hourAgo, // < 48h ago -> VISIBLE
                isRead = false
            ),
            ChatMessage(
                id = "m5",
                senderId = "mom",
                senderName = "Mom ❤️",
                receiverId = "self",
                messageType = MessageType.TEXT,
                textContent = "Call me when you leave work!",
                timestamp = now - 600000,
                isRead = false
            )
        )

        val dadChat = listOf(
            ChatMessage(
                id = "d1",
                senderId = "dad",
                senderName = "Dad 👨‍👧‍👦",
                receiverId = "self",
                messageType = MessageType.TEXT,
                textContent = "Hey, did you check the tire pressure on your car?",
                timestamp = dayAgo,
                isRead = true
            ),
            ChatMessage(
                id = "d2",
                senderId = "self",
                senderName = "You",
                receiverId = "dad",
                messageType = MessageType.TEXT,
                textContent = "All good Dad, filled them up yesterday 👍",
                timestamp = dayAgo + 1800000,
                isRead = true
            ),
            ChatMessage(
                id = "d3",
                senderId = "dad",
                senderName = "Dad 👨‍👧‍👦",
                receiverId = "self",
                messageType = MessageType.IMAGE,
                textContent = "Old family trip photo from 2018 🏔️",
                mediaUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop&q=80",
                timestamp = threeDaysAgo + 3600000, // EXPIRED
                isRead = true
            )
        )

        val grandmaChat = listOf(
            ChatMessage(
                id = "g1",
                senderId = "grandma",
                senderName = "Grandma 👵",
                receiverId = "self",
                messageType = MessageType.TEXT,
                textContent = "God bless you my dear! Here are the cookies I baked 🍪",
                timestamp = now - 7200000,
                isRead = true
            ),
            ChatMessage(
                id = "g2",
                senderId = "grandma",
                senderName = "Grandma 👵",
                receiverId = "self",
                messageType = MessageType.IMAGE,
                textContent = "Fresh out of the oven! 🍪",
                mediaUrl = "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=600&auto=format&fit=crop&q=80",
                timestamp = now - 3600000, // VISIBLE
                isRead = false
            )
        )

        _messagesMap.value = mapOf(
            "mom" to momChat,
            "dad" to dadChat,
            "grandma" to grandmaChat
        )
    }

    fun getMessagesForMember(memberId: String): List<ChatMessage> {
        return _messagesMap.value[memberId] ?: emptyList()
    }

    fun markMessagesAsRead(memberId: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        var updatedAny = false

        val updatedMessages = currentMessages.map { msg ->
            if (msg.senderId != "self" && !msg.isRead) {
                updatedAny = true
                val readMsg = msg.copy(isRead = true)

                // Sync read status to Firestore
                try {
                    firestore?.collection("family_chats")
                        ?.document(memberId)
                        ?.collection("messages")
                        ?.document(msg.id)
                        ?.update("isRead", true)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating read receipt in Firestore: ${e.localizedMessage}")
                }

                readMsg
            } else {
                msg
            }
        }

        if (updatedAny) {
            val updatedMap = _messagesMap.value.toMutableMap()
            updatedMap[memberId] = updatedMessages
            _messagesMap.value = updatedMap
        }

        // Reset unread count for member in list and Firestore
        val member = _familyMembers.value.firstOrNull { it.id == memberId }
        if (member != null && member.unreadCount > 0) {
            val updatedMembers = _familyMembers.value.map { m ->
                if (m.id == memberId) m.copy(unreadCount = 0) else m
            }
            _familyMembers.value = updatedMembers

            try {
                firestore?.collection("family_members")
                    ?.document(memberId)
                    ?.update("unreadCount", 0)
            } catch (e: Exception) {
                Log.w(TAG, "Error resetting unread count in Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun toggleMessageReaction(memberId: String, messageId: String, reactionEmoji: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        val updatedMessages = currentMessages.map { msg ->
            if (msg.id == messageId) {
                val newReaction = if (msg.reaction == reactionEmoji) null else reactionEmoji
                val updatedMsg = msg.copy(reaction = newReaction)
                
                try {
                    firestore?.collection("family_chats")
                        ?.document(memberId)
                        ?.collection("messages")
                        ?.document(messageId)
                        ?.update("reaction", newReaction)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating reaction in Firestore: ${e.localizedMessage}")
                }
                
                updatedMsg
            } else {
                msg
            }
        }
        
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = updatedMessages
        _messagesMap.value = updatedMap
    }

    fun toggleStarMessage(memberId: String, messageId: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        val updatedMessages = currentMessages.map { msg ->
            if (msg.id == messageId) {
                val newStarred = !msg.isStarred
                val updatedMsg = msg.copy(isStarred = newStarred)
                try {
                    firestore?.collection("family_chats")
                        ?.document(memberId)
                        ?.collection("messages")
                        ?.document(messageId)
                        ?.update("isStarred", newStarred)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating star in Firestore: ${e.localizedMessage}")
                }
                updatedMsg
            } else {
                msg
            }
        }
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = updatedMessages
        _messagesMap.value = updatedMap
    }

    fun togglePinMessage(memberId: String, messageId: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        val updatedMessages = currentMessages.map { msg ->
            if (msg.id == messageId) {
                val newPinned = !msg.isPinned
                val updatedMsg = msg.copy(isPinned = newPinned)
                try {
                    firestore?.collection("family_chats")
                        ?.document(memberId)
                        ?.collection("messages")
                        ?.document(messageId)
                        ?.update("isPinned", newPinned)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating pin message in Firestore: ${e.localizedMessage}")
                }
                updatedMsg
            } else {
                msg
            }
        }
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = updatedMessages
        _messagesMap.value = updatedMap
    }

    fun togglePinMember(memberId: String) {
        val updatedMembers = _familyMembers.value.map { member ->
            if (member.id == memberId) {
                val newPinned = !member.isPinned
                try {
                    firestore?.collection("family_members")
                        ?.document(memberId)
                        ?.update("isPinned", newPinned)
                } catch (e: Exception) {
                    Log.w(TAG, "Error pinning member in Firestore: ${e.localizedMessage}")
                }
                member.copy(isPinned = newPinned)
            } else {
                member
            }
        }
        _familyMembers.value = updatedMembers
    }

    fun sendMessage(
        memberId: String,
        textContent: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        forcedTimestamp: Long = System.currentTimeMillis(),
        replyToMessageId: String? = null,
        replyToSenderName: String? = null,
        replyToText: String? = null
    ) {
        val newMessage = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = "self",
            senderName = "You",
            receiverId = memberId,
            messageType = type,
            textContent = textContent,
            mediaUrl = mediaUrl,
            timestamp = forcedTimestamp,
            replyToMessageId = replyToMessageId,
            replyToSenderName = replyToSenderName,
            replyToText = replyToText
        )

        val currentList = (_messagesMap.value[memberId] ?: emptyList()).toMutableList()
        currentList.add(newMessage)

        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = currentList
        _messagesMap.value = updatedMap

        // Sync to Firebase Firestore if available
        try {
            firestore?.collection("family_chats")
                ?.document(memberId)
                ?.collection("messages")
                ?.document(newMessage.id)
                ?.set(newMessage)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync skipped: ${e.localizedMessage}")
        }
    }

    fun toggle48HourFastForward() {
        if (_simulatedTimeOffsetMs.value == 0L) {
            // Fast forward 50 hours into future
            _simulatedTimeOffsetMs.value = 50 * 60 * 60 * 1000L
        } else {
            // Reset to real time
            _simulatedTimeOffsetMs.value = 0L
        }
    }

    fun addExpiredMediaDemo(memberId: String) {
        val fiftyHoursAgo = System.currentTimeMillis() - (50 * 60 * 60 * 1000L)
        sendMessage(
            memberId = memberId,
            textContent = "Demo photo uploaded 50 hours ago",
            type = MessageType.IMAGE,
            mediaUrl = "https://images.unsplash.com/photo-1511895426328-dc8714191300?w=600&auto=format&fit=crop&q=80",
            forcedTimestamp = fiftyHoursAgo
        )
    }
}

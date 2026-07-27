package com.family.talkly.data.models

data class FamilyMember(
    val id: String,
    val name: String,
    val relation: String,
    val avatarUrl: String? = null,
    val status: String = "Available for video call",
    val phone: String,
    val isOnline: Boolean = true,
    val isTyping: Boolean = false,
    val lastSeen: String = "Just now",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isRegisteredOnTalkly: Boolean = false,
    val firebaseUid: String? = null
)

val DEFAULT_FAMILY_MEMBERS = listOf(
    FamilyMember(
        id = "mom",
        name = "Mom ❤️",
        relation = "Mother",
        status = "Family comes first! 🏡",
        phone = "+1 555-0191",
        isOnline = true,
        isTyping = false,
        lastSeen = "Online",
        unreadCount = 2
    ),
    FamilyMember(
        id = "dad",
        name = "Dad 👨‍👧‍👦",
        relation = "Father",
        status = "At work, call if urgent 🚗",
        phone = "+1 555-0192",
        isOnline = true,
        isTyping = false,
        lastSeen = "Online",
        unreadCount = 0
    ),
    FamilyMember(
        id = "grandma",
        name = "Grandma 👵",
        relation = "Grandmother",
        status = "Baking cookies today 🍪",
        phone = "+1 555-0193",
        isOnline = false,
        isTyping = false,
        lastSeen = "Today at 09:15 AM",
        unreadCount = 1
    ),
    FamilyMember(
        id = "brother",
        name = "Alex (Brother) 🎮",
        relation = "Brother",
        status = "Studying... or gaming 🎧",
        phone = "+1 555-0194",
        isOnline = true,
        isTyping = false,
        lastSeen = "Online",
        unreadCount = 0
    ),
    FamilyMember(
        id = "sister",
        name = "Sarah (Sister) 🎨",
        relation = "Sister",
        status = "Painting & chilling 🖌️",
        phone = "+1 555-0195",
        isOnline = true,
        isTyping = false,
        lastSeen = "Online",
        unreadCount = 0
    )
)

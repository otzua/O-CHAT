package com.ochat.android.ui.home

import android.net.Uri
import com.ochat.android.model.BitchatMessage
import com.ochat.android.model.BitchatMessageType
import java.util.Date

/**
 * Identifies one row in the conversation list.
 *
 * These are presentation-layer handles only. Each one maps onto state that already exists in
 * ChatState, so the list is a pure projection - nothing here owns or mutates chat data.
 */
sealed interface ConversationId {
    /** A one-to-one chat with a mesh peer or a Nostr/geohash peer. */
    data class Private(val peerID: String) : ConversationId

    /** A joined named channel, stored including its leading '#'. */
    data class Channel(val name: String) : ConversationId

    /** The mesh-wide public timeline. */
    data object MeshPublic : ConversationId

    /** A location channel, keyed by bare geohash (no '#'). */
    data class Geohash(val geohash: String) : ConversationId

    /**
     * Encodes to a single path segment for Navigation Compose.
     *
     * Peer IDs and channel names can contain characters that are meaningful in a route
     * (notably '#' and '/'), so the payload is percent-encoded rather than interpolated raw.
     */
    fun encode(): String = when (this) {
        is MeshPublic -> "mesh"
        is Private -> "private:" + Uri.encode(peerID)
        is Channel -> "channel:" + Uri.encode(name)
        is Geohash -> "geohash:" + Uri.encode(geohash)
    }

    companion object {
        fun decode(raw: String): ConversationId {
            val decoded = Uri.decode(raw)
            return when {
                decoded == "mesh" -> MeshPublic
                decoded.startsWith("private:") -> Private(decoded.removePrefix("private:"))
                decoded.startsWith("channel:") -> Channel(decoded.removePrefix("channel:"))
                decoded.startsWith("geohash:") -> Geohash(decoded.removePrefix("geohash:"))
                else -> MeshPublic
            }
        }
    }
}

/**
 * One rendered row: everything the list needs, already resolved.
 */
data class ConversationRow(
    val id: ConversationId,
    val title: String,
    /** Last-message preview; empty when the conversation has no messages yet. */
    val preview: String,
    val timestamp: Date?,
    val unreadCount: Int,
    /** Drives the avatar colour so a peer keeps the same colour everywhere. */
    val avatarSeed: String,
    val isOnline: Boolean = false,
    val isFavorite: Boolean = false,
    /** Participant count for location channels, shown instead of a preview. */
    val participantCount: Int? = null
)

/**
 * Builds the preview line for a conversation, mirroring how WhatsApp summarises the
 * last message: media becomes a short label rather than a file path, and messages from
 * others in a group context are prefixed with the sender.
 */
fun previewFor(message: BitchatMessage?, includeSender: Boolean, selfNickname: String): String {
    if (message == null) return ""
    val body = when (message.type) {
        BitchatMessageType.Image -> "Photo"
        BitchatMessageType.Audio -> "Voice message"
        BitchatMessageType.File -> "File"
        BitchatMessageType.Message -> message.content.replace('\n', ' ').trim()
    }
    if (!includeSender) return body
    val senderBase = message.sender.substringBefore('#')
    return if (senderBase == selfNickname.substringBefore('#')) "You: $body" else "$senderBase: $body"
}

/** Newest conversations first; rows that have never had a message sink to the bottom. */
fun List<ConversationRow>.sortedForDisplay(): List<ConversationRow> =
    sortedWith(
        compareByDescending<ConversationRow> { it.timestamp?.time ?: Long.MIN_VALUE }
            .thenBy { it.title.lowercase() }
    )

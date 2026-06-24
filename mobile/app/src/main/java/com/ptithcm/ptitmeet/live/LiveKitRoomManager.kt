package com.ptithcm.ptitmeet.live

import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.DataPublishReliability
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import io.livekit.android.events.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class LiveKitRoomManager(private val context: Context) {

    interface Listener {
        fun onConnected()
        fun onConnectionError(message: String)
        fun onDisconnected()
        fun onParticipantsUpdated(participants: List<LiveParticipantState>)
        fun onReactionReceived(senderId: String, senderName: String, emoji: String)
        fun onHandRaiseReceived(senderId: String, isRaised: Boolean)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var room: Room? = null
    private var listener: Listener? = null
    private var eventsJob: Job? = null
    private val rendererMap = ConcurrentHashMap<String, SurfaceViewRenderer>()
    private val isHandRaisedMap = ConcurrentHashMap<String, Boolean>()

    fun connect(serverUrl: String, token: String, enableMic: Boolean, enableCamera: Boolean, listener: Listener) {
        this.listener = listener
        disconnect()

        val newRoom = LiveKit.create(context.applicationContext)
        room = newRoom
        isHandRaisedMap.clear()

        eventsJob = scope.launch {
            launch {
                newRoom.events.collect { event: RoomEvent ->
                    android.util.Log.d("PTITMEET_LIVEKIT", "RoomEvent received: ${event::class.java.simpleName} -> $event")
                    if (event is RoomEvent.DataReceived) {
                        handleDataReceived(event)
                    } else {
                        refreshParticipants()
                    }
                }
            }

            runCatching {
                newRoom.connect(serverUrl, token)
                newRoom.localParticipant.setMicrophoneEnabled(enableMic)
                newRoom.localParticipant.setCameraEnabled(enableCamera)
            }.onSuccess {
                refreshParticipants()
                listener.onConnected()
            }.onFailure {
                listener.onConnectionError(it.message ?: "Unable to connect LiveKit room")
            }
        }
    }

    private fun handleDataReceived(event: RoomEvent.DataReceived) {
        val topic = event.topic
        val payload = event.data
        val participant = event.participant
        val senderId = participant?.identity?.value ?: ""

        if (topic == "hand_raise") {
            try {
                val json = JSONObject(String(payload, Charsets.UTF_8))
                val isRaised = json.optBoolean("isRaised", false)
                if (senderId.isNotEmpty()) {
                    isHandRaisedMap[senderId] = isRaised
                    listener?.onHandRaiseReceived(senderId, isRaised)
                }
                refreshParticipants()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (topic == "reaction") {
            try {
                val json = JSONObject(String(payload, Charsets.UTF_8))
                val emoji = json.optString("emoji", "")
                val senderName = json.optString("senderName", senderId)
                if (emoji.isNotEmpty()) {
                    listener?.onReactionReceived(senderId, senderName, emoji)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun publishHandRaise(isRaised: Boolean) {
        val activeRoom = room ?: return
        val localParticipant = activeRoom.localParticipant
        val localIdentity = localParticipant.identity?.value ?: ""
        if (localIdentity.isNotEmpty()) {
            isHandRaisedMap[localIdentity] = isRaised
            refreshParticipants()
        }

        scope.launch {
            runCatching {
                val json = JSONObject().apply {
                    put("isRaised", isRaised)
                }
                val payload = json.toString().toByteArray(Charsets.UTF_8)
                localParticipant.publishData(
                    data = payload,
                    reliability = DataPublishReliability.RELIABLE,
                    topic = "hand_raise"
                )
            }
        }
    }

    fun publishReaction(emoji: String, senderId: String, senderName: String) {
        val activeRoom = room ?: return
        val localParticipant = activeRoom.localParticipant
        scope.launch {
            runCatching {
                val json = JSONObject().apply {
                    put("emoji", emoji)
                    put("senderId", senderId)
                    put("senderName", senderName)
                }
                val payload = json.toString().toByteArray(Charsets.UTF_8)
                localParticipant.publishData(
                    data = payload,
                    reliability = DataPublishReliability.RELIABLE,
                    topic = "reaction"
                )
            }
        }
    }

    @kotlin.jvm.JvmOverloads
    fun setScreenShareEnabled(enabled: Boolean, mediaProjectionPermissionResultData: Intent? = null) {
        val activeRoom = room ?: return
        scope.launch {
            runCatching {
                val params = mediaProjectionPermissionResultData?.let {
                    ScreenCaptureParams(mediaProjectionPermissionResultData = it)
                }
                activeRoom.localParticipant.setScreenShareEnabled(enabled, params)
                refreshParticipants()
            }
        }
    }

    fun disconnect() {
        eventsJob?.cancel()
        eventsJob = null
        rendererMap.values.forEach { renderer ->
            runCatching { renderer.release() }
        }
        rendererMap.clear()
        room?.disconnect()
        room = null
    }

    fun release() {
        disconnect()
        scope.cancel()
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        val activeRoom = room ?: return
        scope.launch {
            runCatching {
                activeRoom.localParticipant.setMicrophoneEnabled(enabled)
                refreshParticipants()
            }
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        val activeRoom = room ?: return
        scope.launch {
            runCatching {
                activeRoom.localParticipant.setCameraEnabled(enabled)
                refreshParticipants()
            }
        }
    }

    private fun setRendererScalingType(renderer: SurfaceViewRenderer, typeName: String) {
        try {
            val method = renderer.javaClass.methods.firstOrNull { it.name == "setScalingType" }
            if (method != null) {
                val scalingTypeClass = method.parameterTypes.firstOrNull()
                if (scalingTypeClass != null && scalingTypeClass.isEnum) {
                    val constants = scalingTypeClass.enumConstants
                    val targetConstant = constants?.firstOrNull { it.toString() == typeName }
                    if (targetConstant != null) {
                        method.invoke(renderer, targetConstant)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun attachVideo(identity: String, container: FrameLayout) {
        val activeRoom = room ?: return
        val isScreenShare = identity.startsWith("screenshare_")
        val actualIdentity = if (isScreenShare) identity.substring("screenshare_".length) else identity

        val participant = when {
            activeRoom.localParticipant.identity?.value == actualIdentity -> activeRoom.localParticipant
            else -> activeRoom.remoteParticipants[Participant.Identity(actualIdentity)]
        } ?: return

        val targetSource = if (isScreenShare) {
            Track.Source.SCREEN_SHARE
        } else {
            Track.Source.CAMERA
        }

        val videoTrack = participant.videoTrackPublications
            .filter { it.first.source == targetSource }
            .mapNotNull { it.second as? VideoTrack }
            .firstOrNull()
            ?: return

        val renderer = rendererMap[identity] ?: SurfaceViewRenderer(container.context).also {
            activeRoom.initVideoRenderer(it)
            rendererMap[identity] = it
        }

        if (isScreenShare) {
            setRendererScalingType(renderer, "SCALE_ASPECT_FIT")
        } else {
            setRendererScalingType(renderer, "SCALE_ASPECT_FILL")
        }

        if (renderer.parent != container || container.indexOfChild(renderer) == -1) {
            (renderer.parent as? android.view.ViewGroup)?.removeView(renderer)
            container.removeAllViews()
            container.addView(renderer, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        videoTrack.addRenderer(renderer)
    }

    private fun refreshParticipants() {
        val activeRoom = room ?: return
        val allParticipants = buildList {
            val local = activeRoom.localParticipant
            val localIdentity = local.identity?.value ?: ""
            val localHandRaised = isHandRaisedMap[localIdentity] ?: false
            val localScreenSharing = local.videoTrackPublications.any { it.first.source == Track.Source.SCREEN_SHARE }

            add(
                LiveParticipantState(
                    identity = localIdentity,
                    displayName = local.name ?: "You",
                    hasVideo = local.isCameraEnabled,
                    isMicOn = local.isMicrophoneEnabled,
                    isSpeaking = activeRoom.activeSpeakers.any { it.identity == local.identity },
                    isLocal = true,
                    isHandRaised = localHandRaised,
                    isScreenSharing = localScreenSharing,
                )
            )

            if (localScreenSharing) {
                add(
                    LiveParticipantState(
                        identity = "screenshare_$localIdentity",
                        displayName = "${local.name ?: "You"}'s Screen Share",
                        hasVideo = true,
                        isMicOn = false,
                        isSpeaking = false,
                        isLocal = true,
                        isHandRaised = false,
                        isScreenSharing = true,
                    )
                )
            }

            activeRoom.remoteParticipants.values.forEach { participant ->
                val remoteIdentity = participant.identity?.value ?: ""
                participant.videoTrackPublications.forEach { pub ->
                    android.util.Log.d("PTITMEET_LIVEKIT", "Remote participant $remoteIdentity published track ${pub.first.sid} with source ${pub.first.source} and track ${pub.second}")
                }
                val remoteHandRaised = isHandRaisedMap[remoteIdentity] ?: false
                val remoteScreenSharing = participant.videoTrackPublications.any { it.first.source == Track.Source.SCREEN_SHARE }

                add(
                    LiveParticipantState(
                        identity = remoteIdentity,
                        displayName = participant.name ?: participant.identity?.value ?: "",
                        hasVideo = participant.isCameraEnabled,
                        isMicOn = participant.isMicrophoneEnabled,
                        isSpeaking = activeRoom.activeSpeakers.any { it.identity == participant.identity },
                        isLocal = false,
                        isHandRaised = remoteHandRaised,
                        isScreenSharing = remoteScreenSharing,
                    )
                )

                if (remoteScreenSharing) {
                    add(
                        LiveParticipantState(
                            identity = "screenshare_$remoteIdentity",
                            displayName = "${participant.name ?: remoteIdentity}'s Screen Share",
                            hasVideo = true,
                            isMicOn = false,
                            isSpeaking = false,
                            isLocal = false,
                            isHandRaised = false,
                            isScreenSharing = true,
                        )
                    )
                }
            }
        }

        listener?.onParticipantsUpdated(allParticipants)
    }
}

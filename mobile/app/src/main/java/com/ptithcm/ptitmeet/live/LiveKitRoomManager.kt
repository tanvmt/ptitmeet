package com.ptithcm.ptitmeet.live

import android.content.Context
import android.widget.FrameLayout
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import java.util.concurrent.ConcurrentHashMap

class LiveKitRoomManager(private val context: Context) {

    interface Listener {
        fun onConnected()
        fun onConnectionError(message: String)
        fun onDisconnected()
        fun onParticipantsUpdated(participants: List<LiveParticipantState>)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var room: Room? = null
    private var listener: Listener? = null
    private var eventsJob: Job? = null
    private val rendererMap = ConcurrentHashMap<String, SurfaceViewRenderer>()

    fun connect(serverUrl: String, token: String, enableMic: Boolean, enableCamera: Boolean, listener: Listener) {
        this.listener = listener
        disconnect()

        val newRoom = LiveKit.create(context.applicationContext)
        room = newRoom

        eventsJob = scope.launch {
            launch {
                newRoom.events.collectLatest {
                    refreshParticipants()
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

    fun attachVideo(identity: String, container: FrameLayout) {
        val activeRoom = room ?: return
        val participant = when {
            activeRoom.localParticipant.identity == identity -> activeRoom.localParticipant
            else -> activeRoom.remoteParticipants[identity]
        } ?: return

        val videoTrack = participant.videoTrackPublications.values
            .mapNotNull { it.track as? VideoTrack }
            .firstOrNull()
            ?: return

        val renderer = rendererMap[identity] ?: SurfaceViewRenderer(container.context).also {
            activeRoom.initVideoRenderer(it)
            rendererMap[identity] = it
        }

        if (renderer.parent != container) {
            (renderer.parent as? FrameLayout)?.removeView(renderer)
            container.removeAllViews()
            container.addView(renderer)
        }

        videoTrack.addRenderer(renderer)
    }

    private fun refreshParticipants() {
        val activeRoom = room ?: return
        val allParticipants = buildList {
            val local = activeRoom.localParticipant
            add(
                LiveParticipantState(
                    identity = local.identity,
                    displayName = local.name ?: "You",
                    hasVideo = local.isCameraEnabled,
                    isMicOn = local.isMicrophoneEnabled,
                    isSpeaking = activeRoom.activeSpeakers.any { it.identity == local.identity },
                    isLocal = true,
                )
            )

            activeRoom.remoteParticipants.values.forEach { participant ->
                add(
                    LiveParticipantState(
                        identity = participant.identity,
                        displayName = participant.name ?: participant.identity,
                        hasVideo = participant.isCameraEnabled,
                        isMicOn = participant.isMicrophoneEnabled,
                        isSpeaking = activeRoom.activeSpeakers.any { it.identity == participant.identity },
                        isLocal = false,
                    )
                )
            }
        }

        listener?.onParticipantsUpdated(allParticipants)
    }
}

/*
 *
 *  Copyright 2022 Jeluchu
 *
 */

package com.lymors.lycommons.utils.mediaplayer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.jeluchu.jchucomponents.utils.mediaplayer.PlaybackInfoListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 *
 * Author: @Jeluchu
 *
 * This class is used to play music with MediaPlayer
 *
 * Once the [MediaPlayer] is released, it can't be used again, and another one has to be
 * created. In the onStop() method of the Activity the [MediaPlayer] is
 * released. Then in the onStart() of the Activity a new [MediaPlayer]
 * object has to be created. That's why this method is private, and called by load(int) and
 * not the constructor.
 *
 * References [PlayerAdapter] and [PlaybackInfoListener]
 *
 */


class MediaPlayerHolder(context: Context) : PlayerAdapter {

    private val mContext: Context = context.applicationContext
    private var mMediaPlayer: MediaPlayer? = null
    private var mResourceId = ""
    private var mPlaybackInfoListener: PlaybackInfoListener? = null
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekbarPositionUpdateTask: Runnable? = null

    private fun initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setOnCompletionListener {
                stopUpdatingCallbackWithPosition()
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener?.onStateChanged(PlaybackInfoListener.State.COMPLETED)
                    mPlaybackInfoListener?.onPlaybackCompleted()
                }
            }
        }
    }

    override val isPlaying: Boolean
        get() = if (mMediaPlayer != null) mMediaPlayer?.isPlaying == true else false

    override val currentProgress: Float
        get() = if (mMediaPlayer != null) {
            val currentSeconds: Long = ((mMediaPlayer?.currentPosition ?: 0) / 1000).toLong()
            val totalSeconds: Long = ((mMediaPlayer?.duration ?: 0) / 1000).toLong()
            (currentSeconds.toDouble() / totalSeconds * 100).toFloat()
        } else 0F

    override val currentTime: String
        get() = if (mMediaPlayer != null) mMediaPlayer?.currentPosition?.milliSecondsToTimer()
            .orEmpty()
        else ""

    override val totalTime: String
        get() = if (mMediaPlayer != null) mMediaPlayer?.duration?.milliSecondsToTimer().orEmpty()
        else ""

    override fun togglePlaying(isPlaying: Boolean) {
        if (mMediaPlayer != null) {
            when (isPlaying) {
                true -> mMediaPlayer?.pause()
                false -> mMediaPlayer?.start()
            }
        }
    }

    fun setPlaybackInfoListener(listener: PlaybackInfoListener?) {
        mPlaybackInfoListener = listener
    }

    override fun loadMedia(resourceId: Int) {
        mResourceId = "android.resource://${mContext.packageName}/$resourceId"
        initializeMediaPlayer()
        mMediaPlayer?.setDataSource(mContext, Uri.parse(mResourceId))
        mMediaPlayer?.prepare()
        initializeProgressCallback()
    }

    override fun loadMedia(mp3Link: String) {
        mResourceId = mp3Link
        initializeMediaPlayer()
        runCatching {
            mMediaPlayer?.setDataSource(mContext, Uri.parse(mResourceId))
        }.getOrElse { it.message }
        runCatching {
            mMediaPlayer?.prepare()
        }.getOrElse { it.message }
        initializeProgressCallback()
    }

    override fun release() {
        if (mMediaPlayer != null) {
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }


    override fun play() {
        if (mMediaPlayer != null && mMediaPlayer?.isPlaying == false) {
            mMediaPlayer?.start()
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener?.onStateChanged(PlaybackInfoListener.State.PLAYING)
            }
            startUpdatingCallbackWithPosition()
        }
    }

    override fun stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    override fun reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer?.reset()
            loadMedia(mResourceId)
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener?.onStateChanged(PlaybackInfoListener.State.RESET)
            }
            stopUpdatingCallbackWithPosition()
        }
    }

    override fun pause() {
        if (mMediaPlayer != null && mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.pause()
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener?.onStateChanged(PlaybackInfoListener.State.PAUSED)
            }
        }
    }

    override fun seekTo(position: Int) {
        if (mMediaPlayer != null) {
            mMediaPlayer?.seekTo(position)
        }
    }

    private fun startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor()
        }
        if (mSeekbarPositionUpdateTask == null) {
            mSeekbarPositionUpdateTask = Runnable { updateProgressCallbackTask() }
        }
        mExecutor?.scheduleWithFixedDelay(
            mSeekbarPositionUpdateTask,
            0,
            PLAYBACK_POSITION_REFRESH_INTERVAL_MS.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    private fun stopUpdatingCallbackWithPosition() {
        if (mExecutor != null) {
            mExecutor?.shutdownNow()
            mExecutor = null
            mSeekbarPositionUpdateTask = null
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener?.onPositionChanged(0)
            }
        }
    }

    private fun updateProgressCallbackTask() {
        if (mMediaPlayer != null && mMediaPlayer?.isPlaying == true) {
            val currentPosition = mMediaPlayer?.currentPosition
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener?.onPositionChanged(currentPosition ?: 0)
            }
        }
    }

    override fun initializeProgressCallback() {
        val duration = mMediaPlayer?.duration
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener?.onDurationChanged(duration ?: 0)
            mPlaybackInfoListener?.onPositionChanged(0)
        }
    }

    companion object {
        const val PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000
    }

    private fun Int.milliSecondsToTimer(): String {

        var finalTimerString = ""
        val secondsString: String

        val hours = (this / (1000 * 60 * 60))
        val minutes = (this % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (this % (1000 * 60 * 60) % (1000 * 60) / 1000)
        if (hours > 0) finalTimerString = "$hours:"

        secondsString = if (seconds < 10) {
            "0$seconds"
        } else {
            "" + seconds
        }
        finalTimerString = "$finalTimerString$minutes:$secondsString"

        return finalTimerString
    }

}
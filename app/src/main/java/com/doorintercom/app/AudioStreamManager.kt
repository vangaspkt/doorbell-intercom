package com.doorintercom.app

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean

class AudioStreamManager(private val onAudioDataRecorded: (ByteArray) -> Unit) {

    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordThread: Thread? = null

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfigOut)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    // Nhận dữ liệu âm thanh từ micro ESP32 và phát ra loa điện thoại
    fun playAudio(pcmData: ByteArray, offset: Int, length: Int) {
        audioTrack?.write(pcmData, offset, length)
    }

    // Bắt đầu thu âm micro điện thoại khi người dùng nhấn giữ nút
    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording.get()) return
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            channelConfigIn,
            audioFormat,
            minBufferSize * 2
        )

        audioRecord?.startRecording()
        isRecording.set(true)

        recordThread = Thread {
            val buffer = ByteArray(640) // 20ms audio frame ở 16kHz 16bit Mono
            while (isRecording.get()) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readBytes > 0) {
                    val sendBuffer = ByteArray(readBytes)
                    System.arraycopy(buffer, 0, sendBuffer, 0, readBytes)
                    onAudioDataRecorded(sendBuffer)
                }
            }
        }.apply { start() }
    }

    // Dừng thu âm khi thả nút
    fun stopRecording() {
        isRecording.set(false)
        try {
            recordThread?.join(300)
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun release() {
        stopRecording()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}


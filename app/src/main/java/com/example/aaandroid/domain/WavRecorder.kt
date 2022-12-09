package com.example.aaandroid.domain

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord

import android.media.MediaRecorder.*

import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.*


class WavRecorder(path: String) {
    var audioData: ShortArray
    private lateinit var recorder: AudioRecord
    private var bufferSize = 0
    private lateinit var recordingThread: Thread
    private var isRecording = false
    lateinit var bufferData: IntArray
    var bytesRecorded = 0
    private val output: String
    private val filename: String
        get() = output
    private val tempFilename: String
        get() {
            val filepath = Environment.getExternalStorageDirectory().path
            val file = File(filepath, AUDIO_RECORDER_FOLDER)
            if (!file.exists()) {
                file.mkdirs()
            }
            val tempFile = File(filepath, AUDIO_RECORDER_TEMP_FILE)
            if (tempFile.exists()) tempFile.delete()
            return file.absolutePath.toString() + "/" + AUDIO_RECORDER_TEMP_FILE
        }

    fun startRecording() {
        recorder = AudioRecord(
            AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, bufferSize
        )
        val i = recorder.state
        if (i == 1) recorder.startRecording()
        isRecording = true
        recordingThread = Thread({ writeAudioDataToFile() }, "AudioRecorder Thread")
        recordingThread.start()
    }

    private fun writeAudioDataToFile() {
        val data = ByteArray(bufferSize)
        val filename = tempFilename
        val os: FileOutputStream
        try {
            os = FileOutputStream(filename)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return
        }
        var read: Int

        while (isRecording) {
            read = recorder.read(data, 0, bufferSize)
            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                try {
                    os.write(data)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        try {
            os.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        isRecording = false
        val i = recorder.state
        if (i == 1) recorder.stop()
        recorder.release()
        recordingThread.interrupt()
        copyWaveFile(tempFilename, filename)
        deleteTempFile()
    }

    private fun deleteTempFile() {
        val file = File(tempFilename)
        file.delete()
    }

    private fun copyWaveFile(inFilename: String, outFilename: String) {
        val `in`: FileInputStream
        val out: FileOutputStream
        val totalAudioLen: Long
        val totalDataLen: Long
        val longSampleRate: Long = RECORDER_SAMPLERATE.toLong()
        val channels =
            if (RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val byteRate: Long =
            (RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8).toLong()
        val data = ByteArray(bufferSize)
        try {
            `in` = FileInputStream(inFilename)
            Log.d("Record", outFilename)
            var file = File(outFilename)
            if (!file.exists()) {
                file.mkdirs()
                file.delete()
            }
            out = FileOutputStream(outFilename)
            totalAudioLen = `in`.channel.size()
            totalDataLen = totalAudioLen + 36
            writeWaveFileHeader(
                out, totalAudioLen, totalDataLen,
                longSampleRate, channels, byteRate
            )
            while (`in`.read(data) != -1) out.write(data)
            out.flush()
            `in`.close()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun writeWaveFileHeader(
        out: FileOutputStream, totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Long, channels: Int, byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] =
            ((if (RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) 1 else 2) * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = RECORDER_BPP.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        out.write(header, 0, 44)
    }

    companion object {
        private const val RECORDER_BPP = 16
        private const val AUDIO_RECORDER_FOLDER = "AudioRecorder"
        private const val AUDIO_RECORDER_TEMP_FILE = "record_temp.raw"
        private const val RECORDER_SAMPLERATE = 44100
        private val RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
        private val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
    }

    init {
        bufferSize = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING
        ) * 3
        audioData = ShortArray(bufferSize) // short array that pcm data is put
        // into.
        output = path
    }
}
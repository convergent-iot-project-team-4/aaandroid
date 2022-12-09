package com.example.aaandroid

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.aaandroid.databinding.FragmentFirstBinding
import com.example.aaandroid.domain.AttendanceWebSocketListener
import com.example.aaandroid.domain.HelloWebSocketListener.Companion.NORMAL_CLOSURE_STATUS
import com.example.aaandroid.domain.WavRecorder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import java.io.File


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var soundPool: SoundPool
    private var highFreq: Int = 0
    private var lowFreq: Int = 0
    private lateinit var client: OkHttpClient
    private lateinit var webSocket: WebSocket
    private lateinit var wavRecorder: WavRecorder

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wavRecorder =
            WavRecorder(WAV_FILE_PATH)
        loadSoundPool()
        openWebSocket()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        webSocket.cancel()
    }

    private fun openWebSocket() {
        client = OkHttpClient()

        binding.attendanceButton.setOnClickListener {
            val request: Request = Request.Builder()
                .url("ws://10.210.131.68:8000$ATTENDANCE_URN")
                .build()
            val listener = AttendanceWebSocketListener(
                20163062,
                this::recordAudio,
                this::playChirp,
                this::sendWavFile
            )

            webSocket = client.newWebSocket(request, listener)
        }

        binding.lowButton.setOnClickListener {
            playChirp()
        }
        binding.recordButton.setOnClickListener {
            Log.d("Record", "Start recording")
            startRecording()
            Log.d("Record", "recording started")
        }
        binding.stopRecordButton.setOnClickListener {
            Log.d("Record", "Stop recording")
            stopRecording()
            Log.d("Record", "recording stopped")
        }
    }

    private fun loadSoundPool() {
        soundPool =
            SoundPool(
                3,
                AudioManager.STREAM_MUSIC,
                0
            )
        highFreq = soundPool.load(context, R.raw.high_chirp_sound, 1)
        lowFreq = soundPool.load(context, R.raw.low_chirp_sound, 2)

    }

    private fun startRecording() {
        if (checkPermission()) {
            wavRecorder.startRecording()
        } else {
            // if audio recording permissions are
            // not grantsed by user below method will
            // ask for runtime permission for mic and storage.
            requestPermission()
            Log.d("Record", "request permission")
        }
    }

    private fun stopRecording() {
        wavRecorder.stopRecording()
    }

    private fun checkPermission(): Boolean {
        // this method is used to check permission
        val result = context?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        val result1 = context?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.RECORD_AUDIO
            )
        }
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        // this method is used to request the
        // permission for audio recording and storage.
        ActivityCompat.requestPermissions(
            activity as MainActivity,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_AUDIO_PERMISSION_CODE
        )
    }

    private fun playChirp() {
        soundPool.play(lowFreq, 1F, 1F, 0, 0, 1F)
    }

    private fun recordAudio() {
        startRecording()
    }

    private fun sendWavFile() {
        Log.d("Record", "Stop recording")
        stopRecording()
        Log.d("Record", "recording stopped")


        Log.d("FileRead", "start file read")
        val file =
            File(WAV_FILE_PATH)
        val fileBytes = file.readBytes()
        Log.d("FileRead", "reading file has done")
        Log.d("Socket", "start File Send")
        webSocket.send(ByteString.of(fileBytes, 0, fileBytes.size))
        Log.d("Socket", "file has sended")
    }

    companion object {
        private const val REQUEST_AUDIO_PERMISSION_CODE = 1
        private val WAV_FILE_PATH =
            Environment.getExternalStorageDirectory().absolutePath + "/data/path_to_file.wav"
        private const val ATTENDANCE_URN = "/attendance"
    }

}
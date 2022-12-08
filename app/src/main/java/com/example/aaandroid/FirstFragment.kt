package com.example.aaandroid

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.aaandroid.databinding.FragmentFirstBinding
import com.example.aaandroid.domain.WebSocketListener
import com.example.aaandroid.domain.WebSocketListener.Companion.NORMAL_CLOSURE_STATUS
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

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

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSoundPool()
        openWebSocket()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        webSocket.close(NORMAL_CLOSURE_STATUS,null)
        webSocket.cancel()
    }

    fun openWebSocket() {
        client = OkHttpClient()

        val request: Request = Request.Builder()
            .url("ws://10.210.131.68:8000/ws")
            .build()
        val listener = WebSocketListener()


        binding.highButton.setOnClickListener {
            Log.d("info","qwerqwer")
            webSocket = client.newWebSocket(request, listener)
            Log.d("info","asdfadsf")
            Log.d("info","zxcvzxcv")
        }
        binding.lowButton.setOnClickListener {
            webSocket.send("asdasdfasdfasdf")
        }
    }

    fun loadSoundPool() {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        ) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()
            SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(
                    audioAttributes
                )
                .build()
        } else {
            SoundPool(
                3,
                AudioManager.STREAM_MUSIC,
                0
            )
        }
        highFreq = soundPool.load(context,R.raw.high_chirp_sound,1)
        lowFreq = soundPool.load(context,R.raw.low_chirp_sound,2)

    }


}
package com.example.aaandroid.domain

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class FileWebSocketListener() : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("Socket", text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d("Socket", bytes.toString())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("Socket", code.toString())
        Log.d("Socket", reason)
        webSocket.close(HelloWebSocketListener.NORMAL_CLOSURE_STATUS, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("Socket", code.toString())
        Log.d("Socket", reason)
    }
}
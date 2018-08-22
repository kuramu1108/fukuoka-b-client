package com.line.fukuokabclient.websocket

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.line.fukuokabclient.dto.MessageDTO
import org.java_websocket.WebSocket
import rx.Observable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.client.StompClient

class WebSocketChatClient (val activity: Activity) {
    private var stompClient: StompClient = Stomp.over(WebSocket::class.java, "ws://ec2-52-194-226-224.ap-northeast-1.compute.amazonaws.com/chat")
    private val breakLine = System.lineSeparator()
    enum class Type {
        OPENED, CLOSED, ERROR
    }

    fun connect() = stompClient.connect()

    fun topic(destination: String, view: TextView) {
        stompClient.topic(destination).subscribe { topicMessage ->
            run {
                val mapper = jacksonObjectMapper()
                val message = mapper.readValue<MessageDTO>(topicMessage.payload)
                activity.runOnUiThread {
                    if (message.senderId.toString() == "9999") {
                        view.append("9999: " + message.text)
                        view.append(breakLine)
                        view.setTextColor(Color.parseColor("#00B900"))
                        view.gravity = Gravity.RIGHT
                    } else {
                        view.setTextColor(Color.parseColor("#ffffff"))
                        view.gravity = Gravity.LEFT
                    }
                }
            }
        }
    }

    fun tooic(destination: String) :Observable<MessageDTO> {
        val mapper = jacksonObjectMapper()
        return stompClient.topic(destination).map { topicMessage ->
            run {
                Log.d("hogehoge", "${topicMessage.payload}")
                mapper.readValue<MessageDTO>(topicMessage.payload)
            }
        }
    }

    fun lifecycle() {
        stompClient.lifecycle().subscribe{ lifecycleEvent ->
            when (lifecycleEvent.type) {

                Type.OPENED -> Log.i(javaClass.simpleName, "WSサーバに接続しました。")

                Type.ERROR -> Log.i(javaClass.simpleName, "エラーが発生しました。")

                Type.CLOSED -> Log.i(javaClass.simpleName, "WSサーバから切断しました。")
            }
        }
    }

    fun disconnect() = stompClient.disconnect()

    fun send(destination: String, message: String) = stompClient.send(destination, message).subscribe()
}
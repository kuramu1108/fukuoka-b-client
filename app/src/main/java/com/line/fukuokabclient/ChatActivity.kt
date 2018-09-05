package com.line.fukuokabclient

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.line.fukuokabclient.Adapter.ChatAdapter
import com.line.fukuokabclient.Utility.Prefs
import com.line.fukuokabclient.Client.Response.ResponsePersonalChannelInfo
import com.line.fukuokabclient.dto.MessageDTO
import com.line.fukuokabclient.websocket.WebSocketChatClient
import kotlinx.android.synthetic.main.activity_chat.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class ChatActivity : AppCompatActivity() {
    private var client = WebSocketChatClient(this)
    var mAuth: FirebaseAuth? = null
    var email:String = ""
    var channelId: Long = 0
    var senderId:Long = 0

    var items = ArrayList<MessageDTO>()
    var messageAdapter: ChatAdapter? = null
    var info: ResponsePersonalChannelInfo? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        setSupportActionBar(chat_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mAuth = FirebaseAuth.getInstance()
        channelId = intent.getLongExtra("channelId", 0)
        senderId = Prefs.get(applicationContext)
                .getLong("id", 0)

        btnSendMessage.setOnClickListener {
            if (client.isConnected() && editSendMessage.text.toString().isNotEmpty() ) {
                val message = MessageDTO(null, senderId, channelId, editSendMessage.text.toString(), null)
                client.send("/app/chat.$channelId", message.toString())

                // メッセージ送信後は入力欄を空欄にする
                editSendMessage.setText("")
            }
        }

        if (intent.getParcelableArrayExtra("messages") != null) items = ArrayList(intent.getParcelableArrayExtra("messages").toList()) as ArrayList<MessageDTO>
        if (intent.getParcelableExtra<ResponsePersonalChannelInfo>("info") != null) info = intent.getParcelableExtra("info")

        this.title = info?.friend?.name?: "yoyo"
        messageAdapter = ChatAdapter(info, items, senderId)
    }

    override fun onStart() {
        super.onStart()
        email = mAuth?.currentUser?.email ?: throw Exception("Not Logged in")
    }

    override fun onResume() {
        super.onResume()

        client.connect()
        start()
        client.lifecycle()
    }

    override fun onPause() {
        super.onPause()
        client.disconnect()
        client = WebSocketChatClient(this)
    }

    fun start() {
        chat_recycler_view.layoutManager = LinearLayoutManager(this)
        chat_recycler_view.adapter = messageAdapter!!

        if (messageAdapter!!.itemCount > 0) chat_recycler_view.scrollToPosition(messageAdapter!!.itemCount - 1)

        client.topic("/topic/chat.$channelId")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("hogehoge", "${items.size}")
                    items.add(it)
                    messageAdapter!!.notifyDataSetChanged()
                    chat_recycler_view.scrollToPosition(messageAdapter!!.itemCount-1)
                }, {
                    Log.e("hogehoge", "error", it)
                }, {
                    Log.d("hogehoge", "completed")
                })
    }



}

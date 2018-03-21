package kumamotone.sample.eremotethingssample

import android.app.Activity
import android.os.Bundle
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class MainActivity : Activity() {
    private fun String.hexStringToByteArray(): ByteArray =
            ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private suspend fun sendToRemocon(data: String) {
        withContext(CommonPool) {
            sendToRemoconWithoutCoroutine(data)
        }
    }

    private fun sendToRemoconWithoutCoroutine(data: String) {
        DatagramChannel.open().use {
            it.socket().bind(InetSocketAddress(9999 /* 送り元ポート */))
            val buf = ByteBuffer.allocate(data.length).also {
                it.clear()
                it.put(data.hexStringToByteArray())
                it.flip()
            }
            it.send(buf, InetSocketAddress(getString(R.string.ipaddress), 80))
        }
    }

    private suspend fun changeLight(words: List<String>) {
        when (words.getOrNull(1)) {
            "の", "を" -> {
                changeLight(words.subList(1, words.count()))
            }
            "つけ", "つけて", "付け", "付けて" -> {
                sendToRemocon(getString(R.string.light_on))
            }
            "けし", "けして", "消し", "消して" -> {
                sendToRemocon(getString(R.string.light_off))
            }
            else -> {
                return
            }
        }
    }

    private suspend fun changeAircon(words: List<String>) {
        when (words.getOrNull(1)) {
            "の", "を" -> {
                changeAircon(words.subList(1, words.count()))
            }
            "つけ", "つけて", "付け", "付けて" -> {
                sendToRemocon(getString(R.string.heat))
            }
            "けし", "けして", "消し", "消して" -> {
                sendToRemocon(getString(R.string.aircon_off))
            }
            "除湿" -> {
                sendToRemocon(getString(R.string.humid))
            }
            else -> {
                return
            }
        }
    }

    private suspend fun oyasumi() {
        sendToRemocon(getString(R.string.light_off))
        sendToRemocon(getString(R.string.aircon_off))
        sendToRemocon(getString(R.string.timer7hours))
    }

    private suspend fun imhome() {
        sendToRemocon(getString(R.string.light_on))
        sendToRemocon(getString(R.string.heat))
    }

    private suspend fun imgoing() {
        sendToRemocon(getString(R.string.light_off))
        sendToRemocon(getString(R.string.aircon_off))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("googlehome").child("word")
        reference.setValue("")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                launch {
                    val str = dataSnapshot.getValue(String::class.java) ?: return@launch
                    val words = str.split(" ")

                    when (words.first()) {
                        "light" -> {
                            changeLight(words)
                        }
                        "aircon" -> {
                            changeAircon(words)
                        }
                        "oyasumi" -> {
                            oyasumi()
                        }
                        "imgoing" -> {
                            imgoing()
                        }
                        "imhome" -> {
                            imhome()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}
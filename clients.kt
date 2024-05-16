import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

val clients = Collections.synchronizedSet(mutableSetOf<ClientHandler>())

fun main() {
    runBlocking {
        val server = ServerSocket(12345)
        println("Server is running on port 12345")
        while (true) {
            val socket = server.accept()
            val clientHandler = ClientHandler(socket)
            clients.add(clientHandler)
            launch {
                clientHandler.run()
            }
        }
    }
}

class ClientHandler(private val socket: Socket) {
    private val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
    private val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
    private lateinit var nickname: String

    suspend fun run() {
        try {
            writer.println("Enter your nickname:")
            nickname = reader.readLine()
            synchronized(clients) {
                while (clients.any { it.nickname == nickname }) {
                    writer.println("Nickname already in use. Enter a different nickname:")
                    nickname = reader.readLine()
                }
            }
            broadcastMessage("[${currentTimestamp()}] $nickname connected")
            var message: String?
            while (reader.readLine().also { message = it } != null) {
                broadcastMessage("[${currentTimestamp()}] $nickname: $message")
            }
        } finally {
            clients.remove(this)
            broadcastMessage("[${currentTimestamp()}] $nickname disconnected")
            socket.close()
        }
    }

    private fun broadcastMessage(message: String) {
        println(message)
        synchronized(clients) {
            clients.forEach {
                it.writer.println(message)
            }
        }
    }

    private fun currentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}

import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

fun main() {
    val server = ChatServer()
    server.startServer()
}

class ChatServer {
    private val clients = ConcurrentHashMap<String, PrintWriter>()

    fun startServer() {
        val serverSocket = ServerSocket(12345)
        println("Server is running on port 12345")

        runBlocking {
            while (true) {
                val socket = serverSocket.accept()
                launch(Dispatchers.IO) {
                    handleClient(socket)
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        var nickname: String? = null
        try {
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            nickname = reader.readLine()

            if (nickname != null && clients.putIfAbsent(nickname, writer) == null) {
                println("$nickname connected")
                writer.println("Welcome $nickname")
                broadcastMessage("$nickname joined the chat", exclude = nickname)

                reader.lines().forEach { message ->
                    broadcastMessage(message, exclude = nickname)
                }
            } else {
                writer.println("Nickname already in use. Try another one.")
                socket.close()
            }
        } catch (e: IOException) {
            println("Error handling client: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                println("Error closing socket: ${e.message}")
            }
            if (nickname != null) {
                clients.remove(nickname)?.close()
                broadcastMessage("$nickname left the chat", exclude = null)
                println("$nickname disconnected")
            }
        }
    }

    private fun broadcastMessage(message: String, exclude: String?) {
        clients.forEach { (nickname, writer) ->
            if (nickname != exclude) {
                writer.println(message)
            }
        }
    }
}

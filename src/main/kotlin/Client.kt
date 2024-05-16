import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import kotlin.random.Random

class ChatApp : Application() {
    private lateinit var writer: PrintWriter
    private lateinit var reader: BufferedReader
    private lateinit var messageArea: VBox
    private lateinit var username: String

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun start(stage: Stage) {
        messageArea = VBox(10.0).apply {
            styleClass.add("message-area")
        }

        val scrollPane = ScrollPane(messageArea).apply {
            isFitToWidth = true
            styleClass.add("text-area")
            vvalueProperty().bind(messageArea.heightProperty())
        }

        val inputField = TextField().apply {
            promptText = "Enter your message..."
            styleClass.add("text-field")
            setOnAction {
                val message = text
                scope.launch {
                    sendMessage(message)
                    Platform.runLater {
                        addMessage("You: $message", true)
                    }
                }
                clear()
            }
        }

        val sendButton = Button("Send").apply {
            styleClass.add("button")
            setOnAction {
                val message = inputField.text
                scope.launch {
                    sendMessage(message)
                    Platform.runLater {
                        addMessage("You: $message", true)
                    }
                }
                inputField.clear()
            }
        }

        val inputContainer = HBox(10.0, inputField, sendButton).apply {
            HBox.setHgrow(inputField, Priority.ALWAYS)
        }

        val root = VBox(10.0, scrollPane, inputContainer).apply {
            styleClass.add("root")
            VBox.setVgrow(scrollPane, Priority.ALWAYS)
        }

        val scene = Scene(root, 600.0, 400.0)
        scene.stylesheets.add(javaClass.getResource("/styles.css").toExternalForm())

        stage.title = "ChatApp"
        stage.scene = scene
        stage.show()

        connectToServer("127.0.0.1", 12345)
    }

    private fun connectToServer(host: String, port: Int) {
        scope.launch {
            val socket = Socket(host, port)
            writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            username = generateUsername()
            writer.println(username)

            reader.lines().forEach { message ->
                Platform.runLater {
                    addMessage(message, false)
                }
            }
        }
    }

    private suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            writer.println("$username: $message")
        }
    }

    private fun generateUsername(): String {
        return "User" + Random.nextInt(1000, 9999)
    }

    private fun addMessage(message: String, isUser: Boolean) {
        val messageLabel = Label(message).apply {
            styleClass.add("message")
            if (isUser) {
                styleClass.add("you")
            }
            isWrapText = true
        }

        val messageContainer = HBox(messageLabel).apply {
            styleClass.add("message-container")
            if (isUser) {
                styleClass.add("you")
            }
            messageLabel.maxWidthProperty().bind(this.widthProperty().multiply(0.7))
        }

        messageArea.children.add(messageContainer)
    }
}

fun main() {
    Application.launch(ChatApp::class.java)
}

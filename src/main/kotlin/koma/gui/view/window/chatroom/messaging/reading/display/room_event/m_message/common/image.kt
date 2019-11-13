package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common

import javafx.beans.binding.Bindings
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.stage.Stage
import koma.Server
import koma.gui.dialog.file.save.downloadFileAs
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.network.media.MHUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.component.MxcImageView
import link.continuum.desktop.util.gui.alert
import mu.KotlinLogging
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

private val logger = KotlinLogging.logger {}

class ImageElement(
        private val imageSize: Double = 200.0
) : ViewNode, CoroutineScope by CoroutineScope(Dispatchers.Default) {
    override val node = StackPane()
    override val menuItems: List<MenuItem>

    private var imageView = MxcImageView().apply {
        fitHeight = imageSize
        fitWidth = imageSize
    }
    private var url: MHUrl? = null
    private var server: Server? = null

    fun update(mxc: MHUrl, server: Server) {
        imageView.setMxc(mxc, server)
        this.url = mxc
    }

    init {
        node.add(imageView.root)
        node.setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY) {
                BiggerViews.show(url.toString(), imageView.image)
            }
        }

        menuItems = menuItems()
    }

    private fun menuItems(): List<MenuItem> {
        val tm = MenuItem("Save Image")
        tm.setOnAction {
            val u = url?: run {
                alert(
                        Alert.AlertType.ERROR,
                        "Can't download",
                        "url is null"
                )
                return@setOnAction
            }
            val s = server ?: run {
                alert(
                        Alert.AlertType.ERROR,
                        "Can't download",
                        "http client is null"
                )
                return@setOnAction
            }
            downloadFileAs(s.mxcToHttp(u), title = "Save Image As", httpClient = s.httpClient)
        }
        return listOf(tm)
    }
}

object BiggerViews {
    private val views = mutableListOf<View>()

    fun show(title: String, image: Image?) {
        val view = if (views.isEmpty()) {
            logger.info { "creating img viewer window" }
            View()
        } else views.removeAt(views.size - 1)
        view.show(title, image)
    }

    private class View() {
        val root = StackPane()
        private val scene = Scene(root)
        private val stage = Stage().apply {
            isResizable = true
        }
        private var imageView: ImageView
        private val closed = AtomicBoolean(false)

        init {
            stage.scene = scene
            stage.initOwner(JFX.primaryStage)
            imageView = ImageView().apply {
                isPreserveRatio = true
                isSmooth = true
            }
            imageView.fitWidthProperty().bind(
                    Bindings.createDoubleBinding(Callable {
                        max(imageView.image?.width ?: 100.0,
                                root.width)
                    }, imageView.imageProperty(), root.widthProperty()))
            imageView.fitHeightProperty().bind(
                    Bindings.createDoubleBinding(Callable {
                        max(imageView.image?.height ?: 50.0,
                                root.height)
                    }, imageView.imageProperty(), root.heightProperty()))
            root.add(imageView)
            stage.addEventFilter(KeyEvent.KEY_RELEASED) {
                if (it.code != KeyCode.ESCAPE) return@addEventFilter
                it.consume()
                stage.close()
            }
            stage.onHidden = EventHandler{ close() }
        }


        private fun close() {
            imageView.image = null
            if (closed.getAndSet(true)) {
                logger.debug { "image viewer $this already closed" }
                return
            }
            if (views.size < 3) views.add(this)
        }

        fun show(title: String, image: Image?) {
            closed.set(false)
            logger.debug { "viewing image $image" }
            imageView.image = image
            this.stage.title = title
            stage.show()
        }
    }
}

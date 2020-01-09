package koma.gui.view.window.start

import javafx.geometry.Pos
import koma.gui.view.LoginScreen
import koma.koma_app.AppStore
import kotlinx.coroutines.CompletableDeferred
import link.continuum.desktop.database.KeyValueStore
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.StackPane
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.controlsfx.control.MaskerPane
import kotlin.time.ClockMark
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class StartScreen(
        private val startTime: ClockMark
) {

    val root = StackPane()
    private val login = CompletableDeferred<LoginScreen>()
    fun initialize(keyValueStore: KeyValueStore) {
        debugAssertUiThread()
        val innerBox = HBox().apply {
            alignment = Pos.CENTER
        }
        root.children.add(VBox().apply {
                    alignment = Pos.CENTER
                    children.add(innerBox)
        })
        val mask = MaskerPane().apply {
            isVisible = false
        }
        root.children.add(mask)
        val l = LoginScreen(keyValueStore, mask)
        innerBox.children.add(l.root)
        login.complete(l)
    }

    suspend fun start(appStore: AppStore, httpClient: OkHttpClient) {
        login.await().start(appStore, httpClient)
    }
}

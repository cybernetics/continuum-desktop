package koma.gui.view.window.chatroom.messaging.reading

import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Priority
import javafx.util.Callback
import koma.Koma
import koma.gui.element.control.KListView
import koma.koma_app.appState
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.list.user.UserDataStore
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*
import kotlin.math.roundToInt
private val logger = KotlinLogging.logger {}
private val settings = appState.store.settings

typealias EventItem =Pair<RoomEventRow, Room>

private class ViewRoomState(
        // whether the user has caught up to the newest messages
        //whether the list should scroll automatically
        val following:SimpleBooleanProperty = SimpleBooleanProperty(true),
        // timestamp of lastest message in the room
        var lastTime: Long = 0
)

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class MessagesListScrollPane(
        private val km: Koma,
        store: UserDataStore
): View() {
    override val root = AnchorPane()

    private val virtualList: KListView<EventItem>

    private var currentViewing: RoomId? = null
    // decide whether to scroll
    private val followingLatest = SimpleBooleanProperty(true)
    private val roomStates = mutableMapOf<RoomId, ViewRoomState>()

    fun scrollPage(scrollDown: Boolean) {
        val scrollRatio = 0.8f
        if (scrollDown) {
            virtualList.flow?.scroll(scrollRatio)
        } else {
            virtualList.flow?.scroll(-scrollRatio)
        }
    }

    fun scrollToBottom() {
        followingLatest.set(true)
        virtualList.items?.let {
            virtualList.scrollTo(it.size - 1)
        }
    }

    fun setList(msgs: ObservableList<EventItem>, roomId: RoomId) {
        virtualList.items = msgs
        currentViewing = roomId
        if (roomStates.containsKey(roomId)) return
        logger.debug { "viewing now room $roomId" }
        val s = ViewRoomState()
        roomStates[roomId] = s
        s.lastTime = Math.max(s.lastTime, msgs.lastOrNull()?.first?.server_time?: 0)
        msgs.addListener { e: ListChangeListener.Change<out EventItem> -> onAddedMessages(e, roomId) }
    }
    init {
        root.vgrow = Priority.ALWAYS

        virtualList = KListView()
        virtualList.vgrow = Priority.ALWAYS
        virtualList.hgrow = Priority.ALWAYS

        virtualList.cellFactory = Callback<ListView<EventItem>, ListCell<EventItem>> {
            RoomEventCell(km, store)
        }

        addVirtualScrollPane()
        addScrollBottomButton()
        scrollToBottom()
        virtualList.addEventFilter(ScrollEvent.SCROLL) { _: ScrollEvent ->
            onScroll()
        }
    }

    private fun addVirtualScrollPane() {
        val virtualScroll = virtualList
        virtualScroll.vgrow = Priority.ALWAYS
        AnchorPane.setTopAnchor(virtualScroll, 0.0)
        AnchorPane.setLeftAnchor(virtualScroll, 0.0)
        AnchorPane.setRightAnchor(virtualScroll, 0.0)
        AnchorPane.setBottomAnchor(virtualScroll, 0.0)
        add(virtualScroll)
    }

    private fun addScrollBottomButton() {
        val scale = settings.scaling
        val button = button() {
            removeWhen(followingLatest)
            graphic = MaterialIconFactory.get().createIcon(MaterialIcon.VERTICAL_ALIGN_BOTTOM, "${(scale*1.5).roundToInt()}em")
            style {
                backgroundRadius = multi(box(15.em))
                val size = scale * 25.px
                minWidth = size
                minHeight = size
                maxWidth = size
                maxHeight = size
            }
            action { scrollToBottom() }
        }
        AnchorPane.setRightAnchor(button, 20.0 * scale)
        AnchorPane.setBottomAnchor(button, 10.0 * scale)
    }

    private fun onAddedMessages(e : ListChangeListener.Change<out EventItem>,
                                roomId: RoomId
    ) {
        val s = roomStates[roomId]
        if (s == null) return
        if (followingLatest.get()) {
            if (e.next() && e.wasAdded()) {
                val added = e.addedSubList
                val lastAdd = added.lastOrNull()?.first?.getEvent()?.origin_server_ts
                if (lastAdd != null && lastAdd > s.lastTime - 5000) {
                    //roughly latest
                    s.lastTime = lastAdd
                    if (added.size > 9) {
                        //added too much
                        followingLatest.set(false)
                    } else if (currentViewing != roomId) {
                        //new message in a not selected room
                        followingLatest.set(false)
                    } else {
                        scrollToBottom()
                    }
                }
            }
        }
    }

    /**
     * remove the scrollToBottom button when the visible area goes downward
     */
    private fun onScroll() {
        val visibleLastIndex = virtualList.visibleIndexRange.value?.endInclusive
        visibleLastIndex ?:return
        val msgList = virtualList.items ?: return
        val loadedLastIndex = msgList.size - 1
        if (visibleLastIndex >= loadedLastIndex - 2) {
            followingLatest.set(true)
        } else {
            followingLatest.set(false)
        }
    }
}

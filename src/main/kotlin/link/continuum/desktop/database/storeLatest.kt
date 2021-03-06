package link.continuum.desktop.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import link.continuum.desktop.observable.MutableObservable
import java.util.concurrent.ConcurrentHashMap

/**
 * save and and get newest values
 * update names and avatar urls in real time
 */
open class LatestFlowMap<K, V>(
        private val init: suspend (K)-> Pair<Long, V>,
        private val save: suspend (K, V, Long) -> Unit
){
    private val scope = CoroutineScope(Dispatchers.Default)
    private val flows = ConcurrentHashMap<K, UpdatableValue>()
    /**
     * set latest value if newer than existing
     */
    suspend fun update(key: K, value: V, time: Long) {
        getUpdatable(key).update(key, value, time)
    }
    fun receiveUpdates(key: K): Flow<V> {
        val up = getUpdatable(key)
        return up.observable.flow()
    }
    private fun getUpdatable(key: K): UpdatableValue {
        return flows.computeIfAbsent(key) { UpdatableValue().also {
            scope.launch {
                val (t, v) = init(key)
                it.update(key, v, t)
            }
        }}
    }
    private inner class UpdatableValue() {
        private val mutex = Mutex()
        private var timestamp = 0L
        val observable = MutableObservable<V>()
        suspend fun update(key: K, value: V, time: Long) {
            mutex.withLock {
                if (time < timestamp) return
                if (timestamp == time && observable.getOrNull() == value) return
                timestamp = time
                observable.set(value)
                save(key, value, time)
            }
        }
    }
}
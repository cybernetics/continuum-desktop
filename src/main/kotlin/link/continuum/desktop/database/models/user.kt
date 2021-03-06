package link.continuum.database.models

import io.requery.*
import io.requery.kotlin.desc
import io.requery.kotlin.eq
import koma.matrix.UserId
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Entity
interface UserNickname: Persistable {
    /**
     * user id like @user:matrix.org
     */
    @get:Index()
    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var owner: String

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var nickname: String

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var since: Long
}

@Entity
interface UserAvatar: Persistable {
    /**
     * user id like @user:matrix.org
     */
    @get:Index()
    @get:Column(length = Int.MAX_VALUE, nullable = false)
     var key: String

    /**
     * URL
     */
    @get:Column(length = Int.MAX_VALUE, nullable = false)
     var avatar: String

    @get:Column(length = Int.MAX_VALUE, nullable = false)
    var since: Long
}

@Entity
interface SyncBatchKey: Persistable {
    /**
     * used to get the next batch with sync
     */
    @get:Key
    @get:Column(length = Int.MAX_VALUE)
    var owner: String

    @get:Column(nullable = false, length = Int.MAX_VALUE)
    var batch: String
}

fun saveUserNick(data: KDataStore, userId: UserId, nick: String, timestamp: Long) {
    val d = data.select(UserNickname::class) where (UserNickname::owner.eq(userId.str)
            and UserNickname::nickname.eq(nick)
            and UserNickname::since.eq(timestamp)
            )
    if (d.get().firstOrNull() != null) {
        logger.trace { "already saved nickname $nick of user $userId with time $timestamp" }
        return
    }
    val t: UserNickname = UserNicknameEntity()
    t.owner = userId.str
    t.nickname = nick
    t.since = timestamp
    data.insert(t)
}

fun getLatestNick(data: KDataStore, userId: UserId): UserNickname? {
    return data.select(UserNickname::class)
            .where(UserNickname::owner.eq(userId.str))
            .orderBy(UserNickname::since.desc())
            .get().firstOrNull()
}

fun saveSyncBatchKey(data: KDataStore, userId: UserId, batch: String) {
    logger.trace { "saving sync batch key of user $userId" }
    val t = SyncBatchKeyEntity()
    t.owner = userId.str
    t.batch = batch
    data.upsert(t)
}


fun getSyncBatchKey(data: KDataStore, userId: UserId): String? {
    val k = data.select(SyncBatchKey::class).where(SyncBatchKey::owner.eq(userId.str)).get().firstOrNull()?.batch
    logger.debug { "loaded sync batch key of user $userId: $k" }
    return k
}

fun saveUserAvatar(data: KDataStore, userId: UserId, avatar: String, timestamp: Long) {
    val d = data.select(UserAvatar::class) where (UserAvatar::key.eq(userId.str)
            and UserAvatar::avatar.eq(avatar)
            and UserAvatar::since.eq(timestamp)
            )
    if (d.get().firstOrNull() != null) {
        logger.trace { "already saved Avatar $avatar of user $userId with time $timestamp" }
        return
    }
    val t: UserAvatar = UserAvatarEntity()
    t.key = userId.str
    t.avatar = avatar
    t.since = timestamp
    data.insert(t)
}

fun getLatestAvatar(data: KDataStore, userId: UserId): UserAvatar? {
    return data.select(UserAvatar::class)
            .where(UserAvatar::key.eq(userId.str))
            .orderBy(UserAvatar::since.desc())
            .get().firstOrNull()
}

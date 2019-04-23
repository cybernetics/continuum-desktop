package link.continuum.libutil

inline infix fun<T: Any?> T.`?or?`(action: () -> T?): T? {
    if (this != null) return this
    return action()
}

inline infix fun<T: Any> T?.`?or`(action: () -> T): T {
    if (this != null) return this
    return action()
}
package com.github.saintedlittle.util

/** Minimalistic hierarchical registry utility — handy for future extensions. */
class Registry<T> {
    private val map = LinkedHashMap<String, T>()
    fun register(key: String, value: T) { map[key] = value }
    operator fun get(key: String): T? = map[key]
    fun keys(): Set<String> = map.keys

    class Root {
        private val registries = LinkedHashMap<String, Registry<*>>()
        fun <T> create(name: String): Registry<T> {
            val r = Registry<T>()
            registries[name] = r
            return r
        }
        @Suppress("UNCHECKED_CAST")
        fun <T> get(name: String): Registry<T>? = registries[name] as? Registry<T>
    }
}

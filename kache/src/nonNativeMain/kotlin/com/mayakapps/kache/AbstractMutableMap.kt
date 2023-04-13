package com.mayakapps.kache

// Copied from kotlin.collections.AbstractMutableMap
internal abstract class AbstractMutableMap<K, V> protected constructor() : AbstractMap<K, V>(), MutableMap<K, V> {

    /**
     * A mutable [Map.Entry] shared by several [Map] implementations.
     */
    internal open class SimpleEntry<K, V>(override val key: K, value: V) : MutableMap.MutableEntry<K, V> {
        constructor(entry: Map.Entry<K, V>) : this(entry.key, entry.value)

        private var _value = value

        override val value: V get() = _value

        override fun setValue(newValue: V): V {
            // Should check if the map containing this entry is mutable.
            // However, to not increase entry memory footprint it might be worthwhile not to check it here and
            // force subclasses that implement `build()` (freezing) operation to implement their own `MutableEntry`.
//            this@AbstractMutableMap.checkIsMutable()
            val oldValue = this._value
            this._value = newValue
            return oldValue
        }

        override fun hashCode(): Int = entryHashCode(this)
        override fun toString(): String = entryToString(this)
        override fun equals(other: Any?): Boolean = entryEquals(this, other)

    }

    // intermediate abstract class to workaround KT-43321
    internal abstract class AbstractEntrySet<E : Map.Entry<K, V>, K, V> : AbstractMutableSet<E>() {
        final override fun contains(element: E): Boolean = containsEntry(element)
        abstract fun containsEntry(element: Map.Entry<K, V>): Boolean
        final override fun remove(element: E): Boolean = removeEntry(element)
        abstract fun removeEntry(element: Map.Entry<K, V>): Boolean
    }

    override fun clear() {
        entries.clear()
    }

    private var _keys: MutableSet<K>? = null
    override val keys: MutableSet<K>
        get() {
            if (_keys == null) {
                _keys = object : AbstractMutableSet<K>() {
                    override fun add(element: K): Boolean = throw UnsupportedOperationException("Add is not supported on keys")
                    override fun clear() {
                        this@AbstractMutableMap.clear()
                    }

                    override operator fun contains(element: K): Boolean = containsKey(element)

                    override operator fun iterator(): MutableIterator<K> {
                        val entryIterator = entries.iterator()
                        return object : MutableIterator<K> {
                            override fun hasNext(): Boolean = entryIterator.hasNext()
                            override fun next(): K = entryIterator.next().key
                            override fun remove() = entryIterator.remove()
                        }
                    }

                    override fun remove(element: K): Boolean {
                        checkIsMutable()
                        if (containsKey(element)) {
                            this@AbstractMutableMap.remove(element)
                            return true
                        }
                        return false
                    }

                    override val size: Int get() = this@AbstractMutableMap.size

                    fun checkIsMutable(): Unit = this@AbstractMutableMap.checkIsMutable()
                }
            }
            return _keys!!
        }

    abstract override fun put(key: K, value: V): V?

    override fun putAll(from: Map<out K, V>) {
        checkIsMutable()
        for ((key, value) in from) {
            put(key, value)
        }
    }

    private var _values: MutableCollection<V>? = null
    override val values: MutableCollection<V>
        get() {
            if (_values == null) {
                _values = object : AbstractMutableCollection<V>() {
                    override fun add(element: V): Boolean = throw UnsupportedOperationException("Add is not supported on values")
                    override fun clear() = this@AbstractMutableMap.clear()

                    override operator fun contains(element: V): Boolean = containsValue(element)

                    override operator fun iterator(): MutableIterator<V> {
                        val entryIterator = entries.iterator()
                        return object : MutableIterator<V> {
                            override fun hasNext(): Boolean = entryIterator.hasNext()
                            override fun next(): V = entryIterator.next().value
                            override fun remove() = entryIterator.remove()
                        }
                    }

                    override val size: Int get() = this@AbstractMutableMap.size

                    fun checkIsMutable(): Unit = this@AbstractMutableMap.checkIsMutable()
                }
            }
            return _values!!
        }

    override fun remove(key: K): V? {
        checkIsMutable()
        val iter = entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val k = entry.key
            if (key == k) {
                val value = entry.value
                iter.remove()
                return value
            }
        }
        return null
    }


    /**
     * This method is called every time when a mutating method is called on this mutable map.
     * Mutable maps that are built (frozen) must throw `UnsupportedOperationException`.
     */
    internal open fun checkIsMutable(): Unit {}
}
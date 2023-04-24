/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package com.mayakapps.kache.collections

// MODIFICATION: comment out freezing staff
//import kotlin.native.concurrent.isFrozen
//import kotlin.native.FreezingIsDeprecated

// MODIFICATION: comment out freezing staff
//@OptIn(FreezingIsDeprecated::class)
// MODIFICATION: renamed to LinkedHashMap
class LinkedHashMap<K, V> private constructor(
    private var keysArray: Array<K>,
    private var valuesArray: Array<V>?, // allocated only when actually used, always null in pure HashSet
    private var presenceArray: IntArray,
    private var hashArray: IntArray,
    private var maxProbeDistance: Int,
    private var length: Int,
    private val accessOrder: Boolean, // MODIFICATION: added accessOrder
    private val reverseOrder: Boolean, // MODIFICATION: added reverseOrder
) : MutableMap<K, V> {
    private var hashShift: Int = computeShift(hashSize)

    private var _size: Int = 0
    override val size: Int
        get() = _size

    private var keysView: HashMapKeys<K>? = null
    private var valuesView: HashMapValues<V>? = null
    private var entriesView: HashMapEntrySet<K, V>? = null

    private var isReadOnly: Boolean = false

    // ---------------------------- functions ----------------------------

    constructor() : this(INITIAL_CAPACITY)

    constructor(initialCapacity: Int) : this(
        arrayOfUninitializedElements(initialCapacity),
        null,
        IntArray(initialCapacity),
        IntArray(computeHashSize(initialCapacity)),
        INITIAL_MAX_PROBE_DISTANCE,
        0, false, false
    )

    constructor(original: Map<out K, V>) : this(original.size) {
        putAll(original)
    }

    // This implementation doesn't use a loadFactor, this constructor is used for compatibility with common stdlib
    constructor(initialCapacity: Int, loadFactor: Float, accessOrder: Boolean, reverseOrder: Boolean) : this(
        arrayOfUninitializedElements(initialCapacity),
        null,
        IntArray(initialCapacity),
        IntArray(computeHashSize(initialCapacity)),
        INITIAL_MAX_PROBE_DISTANCE,
        0, accessOrder, reverseOrder
    )

    @PublishedApi
    internal fun build(): Map<K, V> {
        checkIsMutable()
        isReadOnly = true
        return this
    }

    override fun isEmpty(): Boolean = _size == 0
    override fun containsKey(key: K): Boolean = findKey(key) >= 0
    override fun containsValue(value: V): Boolean = findValue(value) >= 0

    override operator fun get(key: K): V? {
        // MODIFICATION: added accessOrder logic
        if (accessOrder) {
            val value = remove(key)
            if (value != null) put(key, value)
            return value
        }

        val index = findKey(key)
        if (index < 0) return null
        return valuesArray!![index]
    }

    override fun put(key: K, value: V): V? {
        checkIsMutable()
        val index = addKey(key)
        val valuesArray = allocateValuesArray()
        if (index < 0) {
            // MODIFICATION: added accessOrder logic
            if (accessOrder) {
                val oldValue = remove(key)
                put(key, value)
                return oldValue
            }

            val oldValue = valuesArray[-index - 1]
            valuesArray[-index - 1] = value
            return oldValue
        } else {
            valuesArray[index] = value
            return null
        }
    }

    override fun putAll(from: Map<out K, V>) {
        checkIsMutable()
        putAllEntries(from.entries)
    }

    override fun remove(key: K): V? {
        val index = removeKey(key)  // mutability gets checked here
        if (index < 0) return null
        val valuesArray = valuesArray!!
        val oldValue = valuesArray[index]
        valuesArray.resetAt(index)
        return oldValue
    }

    override fun clear() {
        checkIsMutable()
        // O(length) implementation for hashArray cleanup
        for (i in 0..length - 1) {
            val hash = presenceArray[i]
            if (hash >= 0) {
                hashArray[hash] = 0
                presenceArray[i] = TOMBSTONE
            }
        }
        keysArray.resetRange(0, length)
        valuesArray?.resetRange(0, length)
        _size = 0
        length = 0
    }

    override val keys: MutableSet<K>
        get() {
            val cur = keysView
            return if (cur == null) {
                val new = HashMapKeys(this)
                // MODIFICATION: comment out freezing staff
//            if (!isFrozen)
                keysView = new
                new
            } else cur
        }

    override val values: MutableCollection<V>
        get() {
            val cur = valuesView
            return if (cur == null) {
                val new = HashMapValues(this)
                // MODIFICATION: comment out freezing staff
//            if (!isFrozen)
                valuesView = new
                new
            } else cur
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            val cur = entriesView
            return if (cur == null) {
                val new = HashMapEntrySet(this)
                // MODIFICATION: comment out freezing staff
//            if (!isFrozen)
                entriesView = new
                new
            } else cur
        }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is Map<*, *>) &&
                contentEquals(other)
    }

    override fun hashCode(): Int {
        var result = 0
        val it = entriesIterator()
        while (it.hasNext()) {
            result += it.nextHashCode()
        }
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder(2 + _size * 3)
        sb.append("{")
        var i = 0
        val it = entriesIterator()
        while (it.hasNext()) {
            if (i > 0) sb.append(", ")
            it.nextAppendString(sb)
            i++
        }
        sb.append("}")
        return sb.toString()
    }

    // ---------------------------- private ----------------------------

    private val capacity: Int get() = keysArray.size
    private val hashSize: Int get() = hashArray.size

    internal fun checkIsMutable() {
        if (isReadOnly) throw UnsupportedOperationException()
    }

    private fun ensureExtraCapacity(n: Int) {
        if (shouldCompact(extraCapacity = n)) {
            rehash(hashSize)
        } else {
            ensureCapacity(length + n)
        }
    }

    private fun shouldCompact(extraCapacity: Int): Boolean {
        val spareCapacity = this.capacity - length
        val gaps = length - size
        return spareCapacity < extraCapacity                // there is no room for extraCapacity entries
                && gaps + spareCapacity >= extraCapacity    // removing gaps prevents capacity expansion
                && gaps >= this.capacity / 4                // at least 25% of current capacity is occupied by gaps
    }

    private fun ensureCapacity(capacity: Int) {
        if (capacity < 0) throw OutOfMemoryError()    // overflow
        if (capacity > this.capacity) {
            var newSize = this.capacity * 3 / 2
            if (capacity > newSize) newSize = capacity
            keysArray = keysArray.copyOfUninitializedElements(newSize)
            valuesArray = valuesArray?.copyOfUninitializedElements(newSize)
            presenceArray = presenceArray.copyOf(newSize)
            val newHashSize = computeHashSize(newSize)
            if (newHashSize > hashSize) rehash(newHashSize)
        }
    }

    private fun allocateValuesArray(): Array<V> {
        val curValuesArray = valuesArray
        if (curValuesArray != null) return curValuesArray
        val newValuesArray = arrayOfUninitializedElements<V>(capacity)
        valuesArray = newValuesArray
        return newValuesArray
    }

    // Null-check for escaping extra boxing for non-nullable keys.
    private fun hash(key: K) = if (key == null) 0 else (key.hashCode() * MAGIC) ushr hashShift

    private fun compact() {
        var i = 0
        var j = 0
        val valuesArray = valuesArray
        while (i < length) {
            if (presenceArray[i] >= 0) {
                keysArray[j] = keysArray[i]
                if (valuesArray != null) valuesArray[j] = valuesArray[i]
                j++
            }
            i++
        }
        keysArray.resetRange(j, length)
        valuesArray?.resetRange(j, length)
        length = j
        //check(length == size) { "Internal invariant violated during compact: length=$length != size=$size" }
    }

    private fun rehash(newHashSize: Int) {
        if (length > _size) compact()
        if (newHashSize != hashSize) {
            hashArray = IntArray(newHashSize)
            hashShift = computeShift(newHashSize)
        } else {
            hashArray.fill(0, 0, hashSize)
        }
        var i = 0
        while (i < length) {
            if (!putRehash(i++)) {
                throw IllegalStateException(
                    "This cannot happen with fixed magic multiplier and grow-only hash array. " +
                            "Have object hashCodes changed?"
                )
            }
        }
    }

    private fun putRehash(i: Int): Boolean {
        var hash = hash(keysArray[i])
        var probesLeft = maxProbeDistance
        while (true) {
            val index = hashArray[hash]
            if (index == 0) {
                hashArray[hash] = i + 1
                presenceArray[i] = hash
                return true
            }
            if (--probesLeft < 0) return false
            if (hash-- == 0) hash = hashSize - 1
        }
    }

    private fun findKey(key: K): Int {
        var hash = hash(key)
        var probesLeft = maxProbeDistance
        while (true) {
            val index = hashArray[hash]
            if (index == 0) return TOMBSTONE
            if (index > 0 && keysArray[index - 1] == key) return index - 1
            if (--probesLeft < 0) return TOMBSTONE
            if (hash-- == 0) hash = hashSize - 1
        }
    }

    private fun findValue(value: V): Int {
        var i = length
        while (--i >= 0) {
            if (presenceArray[i] >= 0 && valuesArray!![i] == value)
                return i
        }
        return TOMBSTONE
    }

    internal fun addKey(key: K): Int {
        checkIsMutable()
        retry@ while (true) {
            var hash = hash(key)
            // put is allowed to grow maxProbeDistance with some limits (resize hash on reaching limits)
            val tentativeMaxProbeDistance = (maxProbeDistance * 2).coerceAtMost(hashSize / 2)
            var probeDistance = 0
            while (true) {
                val index = hashArray[hash]
                if (index <= 0) { // claim or reuse hash slot
                    if (length >= capacity) {
                        ensureExtraCapacity(1)
                        continue@retry
                    }
                    val putIndex = length++
                    keysArray[putIndex] = key
                    presenceArray[putIndex] = hash
                    hashArray[hash] = putIndex + 1
                    _size++
                    if (probeDistance > maxProbeDistance) maxProbeDistance = probeDistance
                    return putIndex
                }
                if (keysArray[index - 1] == key) {
                    return -index
                }
                if (++probeDistance > tentativeMaxProbeDistance) {
                    rehash(hashSize * 2) // cannot find room even with extra "tentativeMaxProbeDistance" -- grow hash
                    continue@retry
                }
                if (hash-- == 0) hash = hashSize - 1
            }
        }
    }

    internal fun removeKey(key: K): Int {
        checkIsMutable()
        val index = findKey(key)
        if (index < 0) return TOMBSTONE
        removeKeyAt(index)
        return index
    }

    private fun removeKeyAt(index: Int) {
        keysArray.resetAt(index)
        removeHashAt(presenceArray[index])
        presenceArray[index] = TOMBSTONE
        _size--
    }

    private fun removeHashAt(removedHash: Int) {
        var hash = removedHash
        var hole = removedHash // will try to patch the hole in hash array
        var probeDistance = 0
        var patchAttemptsLeft = (maxProbeDistance * 2).coerceAtMost(hashSize / 2) // don't spend too much effort
        while (true) {
            if (hash-- == 0) hash = hashSize - 1
            if (++probeDistance > maxProbeDistance) {
                // too far away -- can release the hole, bad case will not happen
                hashArray[hole] = 0
                return
            }
            val index = hashArray[hash]
            if (index == 0) {
                // end of chain -- can release the hole, bad case will not happen
                hashArray[hole] = 0
                return
            }
            if (index < 0) {
                // TOMBSTONE FOUND
                //   - <--- [ TS ] ------ [hole] ---> +
                //             \------------/
                //             probeDistance
                // move tombstone into the hole
                hashArray[hole] = TOMBSTONE
                hole = hash
                probeDistance = 0
            } else {
                val otherHash = hash(keysArray[index - 1])
                // Bad case:
                //   - <--- [hash] ------ [hole] ------ [otherHash] ---> +
                //             \------------/
                //             probeDistance
                if ((otherHash - hash) and (hashSize - 1) >= probeDistance) {
                    // move otherHash into the hole, move the hole
                    hashArray[hole] = index
                    presenceArray[index - 1] = hole
                    hole = hash
                    probeDistance = 0
                }
            }
            // check how long we're patching holes
            if (--patchAttemptsLeft < 0) {
                // just place tombstone into the hole
                hashArray[hole] = TOMBSTONE
                return
            }
        }
    }

    internal fun containsEntry(entry: Map.Entry<K, V>): Boolean {
        val index = findKey(entry.key)
        if (index < 0) return false
        return valuesArray!![index] == entry.value
    }

    internal fun getEntry(entry: Map.Entry<K, V>): MutableMap.MutableEntry<K, V>? {
        val index = findKey(entry.key)
        return if (index < 0 || valuesArray!![index] != entry.value) {
            null
        } else {
            EntryRef(this, index)
        }
    }

    internal fun getKey(key: K): K? {
        val index = findKey(key)
        return if (index >= 0) {
            keysArray[index]!!
        } else {
            null
        }
    }

    private fun contentEquals(other: Map<*, *>): Boolean = _size == other.size && containsAllEntries(other.entries)

    internal fun containsAllEntries(m: Collection<*>): Boolean {
        val it = m.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            try {
                @Suppress("UNCHECKED_CAST") // todo: get rid of unchecked cast here somehow
                if (entry == null || !containsEntry(entry as Map.Entry<K, V>))
                    return false
            } catch (e: ClassCastException) {
                return false
            }
        }
        return true
    }

    private fun putEntry(entry: Map.Entry<K, V>): Boolean {
        val index = addKey(entry.key)
        val valuesArray = allocateValuesArray()
        if (index >= 0) {
            valuesArray[index] = entry.value
            return true
        }

        // MODIFICATION: added accessOrder logic
        if (accessOrder) {
            val oldValue = remove(entry.key)
            putEntry(entry)
            return entry.value != oldValue
        }

        val oldValue = valuesArray[-index - 1]
        if (entry.value != oldValue) {
            valuesArray[-index - 1] = entry.value
            return true
        }
        return false
    }

    private fun putAllEntries(from: Collection<Map.Entry<K, V>>): Boolean {
        if (from.isEmpty()) return false
        ensureExtraCapacity(from.size)
        val it = from.iterator()
        var updated = false
        while (it.hasNext()) {
            if (putEntry(it.next()))
                updated = true
        }
        return updated
    }

    internal fun removeEntry(entry: Map.Entry<K, V>): Boolean {
        checkIsMutable()
        val index = findKey(entry.key)
        if (index < 0) return false
        if (valuesArray!![index] != entry.value) return false
        removeKeyAt(index)
        return true
    }

    internal fun removeValue(element: V): Boolean {
        checkIsMutable()
        val index = findValue(element)
        if (index < 0) return false
        removeKeyAt(index)
        return true
    }

    internal fun keysIterator() = if (!reverseOrder) KeysItr(this) else RevKeysItr(this)
    internal fun valuesIterator() = if (!reverseOrder) ValuesItr(this) else RevValuesItr(this)
    internal fun entriesIterator() = if (!reverseOrder) EntriesItr(this) else RevEntriesItr(this)

    // MODIFICATION: comment out the following line
//    @kotlin.native.internal.CanBePrecreated
    private companion object {
        private const val MAGIC = -1640531527 // 2654435769L.toInt(), golden ratio
        private const val INITIAL_CAPACITY = 8
        private const val INITIAL_MAX_PROBE_DISTANCE = 2
        private const val TOMBSTONE = -1

        private fun computeHashSize(capacity: Int): Int = (capacity.coerceAtLeast(1) * 3).takeHighestOneBit()

        private fun computeShift(hashSize: Int): Int = hashSize.countLeadingZeroBits() + 1
    }

    internal open class Itr<K, V>(
        internal val map: LinkedHashMap<K, V>
    ) {
        internal var index = 0
        internal var lastIndex: Int = -1

        init {
            initNext()
        }

        internal fun initNext() {
            while (index < map.length && map.presenceArray[index] < 0)
                index++
        }

        fun hasNext(): Boolean = index < map.length

        fun remove() {
            map.checkIsMutable()
            map.removeKeyAt(lastIndex)
            lastIndex = -1
        }
    }

    internal class KeysItr<K, V>(map: LinkedHashMap<K, V>) : Itr<K, V>(map), MutableIterator<K> {
        override fun next(): K {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = map.keysArray[lastIndex]
            initNext()
            return result
        }

    }

    // MODIFICATION: add the following interface
    internal interface EntriesIterator<K, V> : MutableIterator<MutableMap.MutableEntry<K, V>> {
        fun nextHashCode(): Int

        fun nextAppendString(sb: StringBuilder)
    }

    internal class ValuesItr<K, V>(map: LinkedHashMap<K, V>) : Itr<K, V>(map), MutableIterator<V> {
        override fun next(): V {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = map.valuesArray!![lastIndex]
            initNext()
            return result
        }
    }

    internal class EntriesItr<K, V>(map: LinkedHashMap<K, V>) : Itr<K, V>(map),
        MutableIterator<MutableMap.MutableEntry<K, V>>, EntriesIterator<K, V> {
        override fun next(): EntryRef<K, V> {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = EntryRef(map, lastIndex)
            initNext()
            return result
        }

        override fun nextHashCode(): Int {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = map.keysArray[lastIndex].hashCode() xor map.valuesArray!![lastIndex].hashCode()
            initNext()
            return result
        }

        override fun nextAppendString(sb: StringBuilder) {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val key = map.keysArray[lastIndex]
            if (key == map) sb.append("(this Map)") else sb.append(key)
            sb.append('=')
            val value = map.valuesArray!![lastIndex]
            if (value == map) sb.append("(this Map)") else sb.append(value)
            initNext()
        }
    }

    // MODIFICATION: add reversed iterators


    internal open class RevItr<K, V>(
        internal val map: LinkedHashMap<K, V>
    ) {
        // MODIFICATION: reverse the next 2 lines
        internal var index = map.length - 1
        internal var lastIndex: Int = map.length

        init {
            initNext()
        }

        internal fun initNext() {
            // MODIFICATION: reverse the next operation
            while (index >= 0 && map.presenceArray[index] < 0)
                index--
        }

        // MODIFICATION: reverse the next line
        fun hasNext(): Boolean = index >= 0

        fun remove() {
            map.checkIsMutable()
            map.removeKeyAt(lastIndex)
            // MODIFICATION: reverse the next line
            lastIndex = map.length
        }
    }

    internal class RevKeysItr<K, V>(map: LinkedHashMap<K, V>) : RevItr<K, V>(map), MutableIterator<K> {
        override fun next(): K {
            // MODIFICATION: reverse the next 2 lines
            if (index < 0) throw NoSuchElementException()
            lastIndex = index--
            val result = map.keysArray[lastIndex]
            initNext()
            return result
        }

    }

    internal class RevValuesItr<K, V>(map: LinkedHashMap<K, V>) : RevItr<K, V>(map), MutableIterator<V> {
        override fun next(): V {
            // MODIFICATION: reverse the next 2 lines
            if (index < 0) throw NoSuchElementException()
            lastIndex = index--
            val result = map.valuesArray!![lastIndex]
            initNext()
            return result
        }
    }

    internal class RevEntriesItr<K, V>(map: LinkedHashMap<K, V>) : RevItr<K, V>(map),
        MutableIterator<MutableMap.MutableEntry<K, V>>, EntriesIterator<K, V> {
        override fun next(): EntryRef<K, V> {
            // MODIFICATION: reverse the next 2 lines
            if (index < 0) throw NoSuchElementException()
            lastIndex = index--
            val result = EntryRef(map, lastIndex)
            initNext()
            return result
        }

        override fun nextHashCode(): Int {
            // MODIFICATION: reverse the next 2 lines
            if (index < 0) throw NoSuchElementException()
            lastIndex = index--
            val result = map.keysArray[lastIndex].hashCode() xor map.valuesArray!![lastIndex].hashCode()
            initNext()
            return result
        }

        override fun nextAppendString(sb: StringBuilder) {
            // MODIFICATION: reverse the next 2 lines
            if (index < 0) throw NoSuchElementException()
            lastIndex = index--
            val key = map.keysArray[lastIndex]
            if (key == map) sb.append("(this Map)") else sb.append(key)
            sb.append('=')
            val value = map.valuesArray!![lastIndex]
            if (value == map) sb.append("(this Map)") else sb.append(value)
            initNext()
        }
    }

    internal class EntryRef<K, V>(
        private val map: LinkedHashMap<K, V>,
        private val index: Int
    ) : MutableMap.MutableEntry<K, V> {
        override val key: K
            get() = map.keysArray[index]

        override val value: V
            get() = map.valuesArray!![index]

        override fun setValue(newValue: V): V {
            map.checkIsMutable()
            val valuesArray = map.allocateValuesArray()
            val oldValue = valuesArray[index]
            valuesArray[index] = newValue
            return oldValue
        }

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> &&
                    other.key == key &&
                    other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}

// MODIFICATION: Remove ", kotlin.native.internal.KonanSet<E>" from the list of interfaces.

internal class HashMapKeys<E> internal constructor(
    private val backing: LinkedHashMap<E, *>
) : MutableSet<E>, AbstractMutableSet<E>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun contains(element: E): Boolean = backing.containsKey(element)

    // MODIFICATION: Remove "override" from the next line.
    fun getElement(element: E): E? = backing.getKey(element)
    override fun clear() = backing.clear()
    override fun add(element: E): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean = backing.removeKey(element) >= 0
    override fun iterator(): MutableIterator<E> = backing.keysIterator()

    override fun removeAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

internal class HashMapValues<V> internal constructor(
    val backing: LinkedHashMap<*, V>
) : MutableCollection<V>, AbstractMutableCollection<V>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun contains(element: V): Boolean = backing.containsValue(element)
    override fun add(element: V): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<V>): Boolean = throw UnsupportedOperationException()
    override fun clear() = backing.clear()
    override fun iterator(): MutableIterator<V> = backing.valuesIterator()
    override fun remove(element: V): Boolean = backing.removeValue(element)

    override fun removeAll(elements: Collection<V>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

// MODIFICATION: Remove ", kotlin.native.internal.KonanSet<E>" from the list of interfaces.

/**
 * Note: intermediate class with [E] `: Map.Entry<K, V>` is required to support
 * [contains] for values that are [Map.Entry] but not [MutableMap.MutableEntry],
 * and probably same for other functions.
 * This is important because an instance of this class can be used as a result of [Map.entries],
 * which should support [contains] for [Map.Entry].
 * For example, this happens when upcasting [MutableMap] to [Map].
 *
 * The compiler enables special type-safe barriers to methods like [contains], which has [UnsafeVariance].
 * Changing type from [MutableMap.MutableEntry] to [E] makes the compiler generate barriers checking that
 * argument `is` [E] (so technically `is` [Map.Entry]) instead of `is` [MutableMap.MutableEntry].
 *
 * See also [KT-42248](https://youtrack.jetbrains.com/issue/KT-42428).
 */
internal abstract class HashMapEntrySetBase<K, V, E : Map.Entry<K, V>> internal constructor(
    val backing: LinkedHashMap<K, V>
) : MutableSet<E>, AbstractMutableSet<E>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun contains(element: E): Boolean = backing.containsEntry(element)

    // MODIFICATION: Remove "override" from the next line.
    fun getElement(element: E): E? = getEntry(element)
    protected abstract fun getEntry(element: Map.Entry<K, V>): E?
    override fun clear() = backing.clear()
    override fun add(element: E): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean = backing.removeEntry(element)
    override fun containsAll(elements: Collection<E>): Boolean = backing.containsAllEntries(elements)

    override fun removeAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

internal class HashMapEntrySet<K, V> internal constructor(
    backing: LinkedHashMap<K, V>
) : HashMapEntrySetBase<K, V, MutableMap.MutableEntry<K, V>>(backing) {

    override fun getEntry(element: Map.Entry<K, V>): MutableMap.MutableEntry<K, V>? = backing.getEntry(element)

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = backing.entriesIterator()
}
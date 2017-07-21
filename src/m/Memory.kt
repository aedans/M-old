package m

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Created by Aedan Smith.
 */

object GlobalMemoryRegistry {
    private var index = 0
    private val registeredNames = mutableListOf<String>()

    fun addAllToTable(symbolTable: SymbolTable) {
        registeredNames.forEachIndexed { i, name -> symbolTable[name] = MemoryLocation.Value(i) }
    }

    operator fun provideDelegate(nothing: Nothing?, property: KProperty<*>) = object : ReadOnlyProperty<Nothing?, Int> {
        init {
            registeredNames.add(property.name)
        }

        val thisIndex = index++
        @Suppress("NAME_SHADOWING")
        override operator fun getValue(thisRef: Nothing?, property: KProperty<*>) = thisIndex
    }
}

class VirtualMemory {
    private val heap = ArrayList<Any?>()
    private fun expand(i: Int) {
        while (heap.size <= i)
            heap.add(null)
    }

    operator fun get(location: Int): Any? = heap[location]
    operator fun set(location: Int, obj: Any) {
        expand(location)
        heap[location] = obj
    }

    tailrec fun malloc(index: Int = 0): Int {
        expand(index)
        return if (heap[index] == null) index else malloc(index + 1)
    }
}

sealed class MemoryLocation : (VirtualMemory) -> Any {
    class Value(val value: Any) : MemoryLocation() {
        override fun invoke(virtualMemory: VirtualMemory): Any = value
        override fun toString() = "Value($value)"
    }

    class HeapPointer(val index: Int) : MemoryLocation() {
        override fun invoke(virtualMemory: VirtualMemory) = virtualMemory[index]!!
        override fun toString() = "HeapPointer($index)"
    }
}

class SymbolTable {
    private val vars = HashMap<String, MemoryLocation>()
    operator fun get(name: String) = vars[name]
    operator fun set(name: String, type: MemoryLocation) = vars.set(name, type)
}

data class Environment(val virtualMemory: VirtualMemory, val symbolTable: SymbolTable) {
    operator fun get(name: String) = symbolTable[name]?.invoke(virtualMemory)
    operator fun set(name: String, obj: Any) {
        val index = virtualMemory.malloc()
        virtualMemory[index] = obj
        symbolTable[name] = MemoryLocation.HeapPointer(index)
    }
}

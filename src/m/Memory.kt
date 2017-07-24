package m

import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Created by Aedan Smith.
 */

object GlobalMemoryRegistry {
    private var index = 0
    operator fun provideDelegate(nothing: Nothing?, unused: KProperty<*>) = object : ReadOnlyProperty<Nothing?, Int> {
        val thisIndex = index++
        override operator fun getValue(thisRef: Nothing?, property: KProperty<*>) = thisIndex
    }
}

sealed class MemoryLocation : (Memory) -> Any {
    class HeapPointer(val index: Int) : MemoryLocation() {
        override fun invoke(memory: Memory) = memory.getHeapValue(index)!!
        override fun toString() = "*h$index"
    }

    class StackPointer(val index: Int) : MemoryLocation() {
        override fun invoke(memory: Memory) = memory.getStackValue(index)
        override fun toString() = "*s$index"
    }
}

interface SymbolTable {
    fun getLocation(name: String): MemoryLocation?
    fun setLocation(name: String, location: MemoryLocation?)
}

interface Memory {
    fun getHeapValue(location: Int): Any?
    fun setHeapValue(location: Int, obj: Any?)

    fun getStackValue(location: Int): Any
    fun push(any: Any)
    fun pop()

    fun malloc(): Int
}

interface Environment : SymbolTable, Memory {
    fun setVar(name: String, obj: Any) {
        val index = malloc()
        setHeapValue(index, obj)
        setLocation(name, MemoryLocation.HeapPointer(index))
    }
}

class GlobalEnvironment(private val container: SymbolTable? = null) : Environment {
    val vars = HashMap<String, MemoryLocation?>()
    override fun getLocation(name: String) = vars[name] ?: container?.getLocation(name)
    override fun setLocation(name: String, location: MemoryLocation?) = vars.set(name, location)

    val stack = ArrayList<Any>()
    override fun getStackValue(location: Int) = stack[stack.size - 1 - location]
    override fun push(any: Any){
        stack.add(any)
    }

    override fun pop() {
        stack.removeAt(stack.size - 1)
    }

    val heap = ArrayList<Any?>()
    private fun expand(i: Int) {
        while (heap.size <= i)
            heap.add(null)
    }

    override fun getHeapValue(location: Int): Any? = heap[location]
    override fun setHeapValue(location: Int, obj: Any?) {
        expand(location)
        heap[location] = obj
    }

    override fun malloc() = malloc(0)
    tailrec fun malloc(index: Int): Int {
        expand(index)
        return if (heap[index] == null) index else malloc(index + 1)
    }
}

package m

sealed class MemoryLocation {
    abstract fun get(memory: Memory): Any
    abstract fun set(memory: Memory, any: Any)

    class HeapPointer(val index: Int) : MemoryLocation() {
        override fun get(memory: Memory) = memory.getHeapValue(index)
        override fun set(memory: Memory, any: Any) = memory.setHeapValue(index, any)
        override fun toString() = "*h$index"
    }

    class StackPointer(val index: Int) : MemoryLocation() {
        override fun get(memory: Memory) = memory.getStackValue(index)
        override fun set(memory: Memory, any: Any) = memory.setStackValue(index, any)
        override fun toString() = "*s$index"
    }
}

class Memory {
    val stack = ArrayList<Any>()
    fun getStackValue(location: Int) = stack[stack.size - 1 - location]
    fun setStackValue(location: Int, any: Any) {
        stack[location] = any
    }

    fun push(any: Any){
        stack.add(any)
    }

    fun pop() {
        stack.removeAt(stack.size - 1)
    }

    val heap = ArrayList<Any>()
    private fun expand(i: Int) {
        while (heap.size <= i)
            heap.add(Nil)
    }

    fun getHeapValue(location: Int): Any = heap[location]
    fun setHeapValue(location: Int, obj: Any) {
        expand(location)
        heap[location] = obj
    }
}

data class RuntimeEnvironment(val symbolTable: SymbolTable, val memory: Memory) {
    fun setVar(name: String, obj: Any) {
        val location = symbolTable.allocateLocation(name)
        symbolTable.setLocation(name, location)
        location.set(memory, obj)
    }
}

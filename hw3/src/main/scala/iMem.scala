import chisel3._
import chisel3.util.experimental.loadMemoryFromFile
import firrtl.annotations.MemoryLoadFileType

class InstructionMemory(size: Int) extends Module {
    val io = IO(new Bundle {
        val addr = Input(UInt(32.W))
        val instrOut = Output(UInt(32.W))
    })
    val instrMem = Mem(size, UInt(32.W))
    loadMemoryFromFile(instrMem, "./src/test/instructions.txt", MemoryLoadFileType.Binary)

    io.instrOut := instrMem(io.addr)
}

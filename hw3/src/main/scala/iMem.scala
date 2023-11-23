import chisel3._
import chisel3.util.experimental.loadMemoryFromFile

class InstructionMemory(size: Int) extends Module {
    val io = IO(new Bundle {
        val addr = Input(UInt(32.W))
        val instrOut = Output(UInt(32.W))
    })
    val instrMem = Mem(size, UInt(32.W))
    loadMemoryFromFile(instrMem, "./src/test/instructions.txt")  // hex format

    io.instrOut := instrMem(io.addr)
}

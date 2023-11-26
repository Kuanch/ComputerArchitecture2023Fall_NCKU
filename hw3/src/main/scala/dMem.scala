import chisel3._

class DataMemory extends Module {
    val io = IO(new Bundle {
        val in_dataAddr = Input(UInt(32.W))
        val in_data = Input(SInt(32.W))
        val in_wd_sel = Input(UInt(1.W))
        val in_rd_sel = Input(UInt(1.W))
        val out_data = Output(SInt(32.W))
    })
    val mem = Mem(1024, SInt(32.W))
    when (io.in_wd_sel === 1.U && io.in_rd_sel === 0.U) {
        mem(io.in_dataAddr) := io.in_data
        io.out_data := DontCare
    }.otherwise {
        io.out_data := mem(io.in_dataAddr)
    }
}
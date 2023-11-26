import chisel3._


class RegisterFile extends Module {
    val io = IO(new Bundle {
        val in_regWrite = Input(UInt(1.W))
        val in_wdata = Input(SInt(32.W))
        val in_waddr = Input(UInt(5.W))
        val in_rs1_sel = Input(UInt(5.W))
        val in_rs2_sel = Input(UInt(5.W))
        val out_rs1 = Output(SInt(32.W))
        val out_rs2 = Output(SInt(32.W))
        val out_a0 = Output(SInt(32.W))
        val out_a1 = Output(SInt(32.W))
    })
    val registers = RegInit(VecInit(Seq.fill(32)(0.S(32.W))))
    io.out_a0 := registers(10)
    io.out_a1 := registers(11)

    when(io.in_regWrite === 1.U) {
        registers(io.in_waddr) := io.in_wdata
        io.out_rs1 := DontCare
        io.out_rs2 := DontCare
    }.otherwise {
        io.in_waddr := DontCare
        io.in_wdata := DontCare
        io.out_rs1 := registers(io.in_rs1_sel)
        io.out_rs2 := registers(io.in_rs2_sel)
    }
}
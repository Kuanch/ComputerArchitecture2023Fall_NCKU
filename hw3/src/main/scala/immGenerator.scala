import chisel3._
import chisel3.util._


class ImmGenerator extends Module {
    val io = IO(new Bundle {
        val in_instr = Input(UInt(32.W))
        // val pc = Input(UInt(32.W))
        val out_s_imm = Output(SInt(32.W))
        val out_sb_imm = Output(SInt(32.W))
        val out_i_imm = Output(SInt(32.W))
        val out_uj_imm = Output(SInt(32.W))
        val out_u_imm = Output(SInt(32.W))
    })

    io.out_s_imm  := Cat(Fill(20, io.in_instr(31)), io.in_instr(31, 25), io.in_instr(11, 7)).asSInt
    io.out_sb_imm := Cat(io.in_instr(31), io.in_instr(7), io.in_instr(30, 25), io.in_instr(11, 8), 0.S).asSInt
    io.out_i_imm := Cat(Fill(20, io.in_instr(31)), io.in_instr(31, 20)).asSInt
    io.out_u_imm  := Cat(Fill(12, 0.U(1.W)), io.in_instr(31, 12)).asSInt
    io.out_uj_imm := Cat(io.in_instr(31), io.in_instr(19, 12), io.in_instr(20), io.in_instr(30, 21), 0.S).asSInt
}

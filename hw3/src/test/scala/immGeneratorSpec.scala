import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ImmGeneratorSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "ImmGenerator"

    it should "generate correct imm" in {
        test(new ImmGenerator) { c =>
            c.io.in_instr.poke("b00000000000100000000010100010011".U)  // addi a0, x0, 1
            c.io.out_i_imm.expect(1.S)

            c.io.in_instr.poke("b00000000101101010000001001100011".U)  // beq a0, a1, 4
            c.io.out_sb_imm.expect(4.S)

            c.io.in_instr.poke("b00000000011000010010011000100011".U)  // sw t1, 12(sp)
            c.io.out_s_imm.expect(12.S)

            c.io.in_instr.poke("b00000111111111111111001010010111".U)  // auipc t0, 0x7fff
            c.io.out_u_imm.expect(32767.S)

            c.io.in_instr.poke("b11111111100111111111001101101111".U)  // jal t1, -8
            c.io.out_uj_imm.expect(-8.S)
        }
    }
}


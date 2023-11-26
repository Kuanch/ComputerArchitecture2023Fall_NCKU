import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class RegisterFileSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "RegisterFile"

    it should "update register when regWrite is 1" in {
        test(new RegisterFile) { c =>
            c.io.in_regWrite.poke(1.U)
            c.io.in_wdata.poke(0x00000001.S)
            c.io.in_waddr.poke(1.U)
            c.clock.step(1)
            c.io.out_rs1.expect(0.S)
            c.io.out_rs2.expect(0.S)
            c.io.in_regWrite.poke(0.U)
            c.clock.step(1)
            c.io.out_rs1.expect(0.S)
            c.io.out_rs2.expect(0.S)
            c.io.in_rs1_sel.poke(1.U)
            c.io.in_rs2_sel.poke(1.U)
            c.clock.step(1)
            c.io.out_rs1.expect(0x00000001.S)
            c.io.out_rs2.expect(0x00000001.S)
        }
    }

    it should "return register value when index passed in" in {
        test(new RegisterFile) { c =>
            c.io.in_regWrite.poke(1.U)
            c.io.in_wdata.poke(0x00000001.S)
            c.io.in_waddr.poke(6.U)
            c.clock.step(1)
            c.io.in_regWrite.poke(1.U)
            c.io.in_wdata.poke(0x00000003.S)
            c.io.in_waddr.poke(7.U)
            c.clock.step(1)

            c.io.in_rs1_sel.poke(6.U)
            c.io.in_rs2_sel.poke(7.U)
            c.io.out_rs1.expect(1.S)
            c.io.out_rs2.expect(3.S)

            c.clock.step(1)

            c.io.in_regWrite.poke(1.U)
            c.io.in_wdata.poke(0x000000010.S)
            c.io.in_waddr.poke(7.U)

            c.clock.step(1)
            
            c.io.in_rs2_sel.poke(7.U)
            c.io.out_rs2.expect(0x00000010.S)
        }
    }
}
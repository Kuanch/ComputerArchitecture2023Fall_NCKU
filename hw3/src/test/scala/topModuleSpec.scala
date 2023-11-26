import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopModuleTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "TopModule"

    it should "perform r-type 3 instructions" in {
        test(new TopModule) { c =>
            c.io.reg_out1.expect(0.S)
            c.io.reg_out2.expect(0.S)
            c.io.fetch_in_debug.expect("b00000000000100000000010100010011".U)   // add a0, x0, 1
            c.clock.step(1)
            c.io.fetch_in_debug.expect("b00000000001000000000010110010011".U)   // add a1, x0, 2
            c.io.reg_out1.expect(1.S)
            c.clock.step(1)
            c.io.reg_out2.expect(2.S)
            c.io.fetch_in_debug.expect("b00000000101101010000010100110011".U)   // add a0, a0, a1
            c.clock.step(1)
            c.io.reg_out1.expect(3.S)
        }
    }
}

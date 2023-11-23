import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProgramCounterSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "ProgramCounter"

    it should "increment by 4 on each clock cycle" in {
        test(new ProgramCounter) { c =>
            for (i <- 0 until 10) {
                c.io.pcOut.expect((4 * i).U)
                c.clock.step(1)
            }
        }
    }

    it should "jump to corresponding address when encounter brenches" in {
        test(new ProgramCounter) { c =>
            c.io.enableBranch.poke(true.B)
            c.io.branchAddr.poke(0x00000004.U)
            c.clock.step(1)
            c.io.pcOut.expect(4.U)
            c.io.enableBranch.poke(false.B)

            c.clock.step(1)
            c.io.enableBranch.poke(true.B)
            c.io.branchAddr.poke(0x00000008.U)
            c.clock.step(1)
            c.io.pcOut.expect(8.U)
            c.io.enableBranch.poke(false.B)

            c.clock.step(1)
            c.io.enableBranch.poke(true.B)
            c.io.branchAddr.poke(0x0000000c.U)
            c.clock.step(1)
            c.io.pcOut.expect(12.U)
        }
    }
}

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InstructionMemorySpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "InstructionMemory"

    it should "output the correct instruction for a given address" in {
        test(new InstructionMemory(1024)) { c => // Assuming the size of the memory is 1024
            val testInstructions = Seq(
                "b00000000000100000000010100010011".U, // addi a0, x0, 1
                "b00000000001000000000010110010011".U, // addi a1, x0, 2
                "b00000000101101010000010100110011".U  // add  a0, a0, a1
            )

            // Test: Fetch each instruction and compare with expected value
            for ((instr, idx) <- testInstructions.zipWithIndex) {
                c.io.addr.poke(idx.U)
                c.io.instrOut.expect(instr)
                c.clock.step(1)
            }
        }
    }
}

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InstructionMemorySpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "InstructionMemory"

    it should "output the correct instruction for a given address" in {
        test(new InstructionMemory(1024)) { c => // Assuming the size of the memory is 1024
            val testInstructions = Seq(
                "b00000000001000000000001010010011".U, // addi a0, x0, 2
                "b00000000000100000000001011010011".U,
                "b00000000101101010000001010110011".U
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

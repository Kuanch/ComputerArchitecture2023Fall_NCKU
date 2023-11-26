import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ALUSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "ALU"

    it should "calculate correct answer" in {
        test(new ALU) { c =>
            // r-type
            // ADD instruction
            c.io.in_aluOp.poke("b000".U)
            c.io.func3.poke("b000".U)
            c.io.func7.poke("b0".U)
            c.io.in_operand_a.poke(1.S)
            c.io.in_operand_b.poke(4.S)
            c.io.out_aluResult.expect(5.S)

            // XOR instruction
            c.io.in_aluOp.poke("b000".U)
            c.io.func3.poke("b100".U)
            c.io.func7.poke("b0".U)
            c.io.in_operand_a.poke(1.S)
            c.io.in_operand_b.poke(1.S)
            c.io.out_aluResult.expect(0.S)

            // SLT instruction
            c.io.in_aluOp.poke("b000".U)
            c.io.func3.poke("b010".U)
            c.io.func7.poke("b0".U)
            c.io.in_operand_a.poke(1.S)
            c.io.in_operand_b.poke(4.S)
            c.io.out_aluResult.expect(1.S)

            // load-type
            c.io.in_aluOp.poke("b100".U)
            c.io.in_operand_a.poke(1.S)
            c.io.in_operand_b.poke(4.S)
            c.io.out_aluResult.expect(5.S)

            // store-type
            c.io.in_aluOp.poke("b101".U)
            c.io.in_operand_a.poke(1.S)
            c.io.in_operand_b.poke(4.S)
            c.io.out_aluResult.expect(5.S)

            // sb-type
            // beq
            c.io.in_aluOp.poke("b010".U)
            c.io.func3.poke("b000".U)
            c.io.func7.poke("b0".U)
            c.io.in_operand_a.poke(5.S)
            c.io.in_operand_b.poke(5.S)
            c.io.out_aluResult.expect(1.S)

            // bge
            c.io.in_aluOp.poke("b010".U)
            c.io.func3.poke("b101".U)
            c.io.func7.poke("b0".U)
            c.io.in_operand_a.poke(3.S)
            c.io.in_operand_b.poke(5.S)
            c.io.out_aluResult.expect(0.S)

            // i-type
            c.io.in_aluOp.poke("b001".U)
            c.io.in_operand_a.poke(1.S)
            c.io.in_operand_b.poke(4.S)
            c.io.out_aluResult.expect(5.S)

            // jalr/jal-type
            c.io.in_aluOp.poke("b011".U)
            c.io.in_operand_a.poke(6.S)
            c.io.in_operand_b.poke(4.S)
            c.io.out_aluResult.expect(10.S)

            // lui-type

            // auipc-type

        }
    }

}


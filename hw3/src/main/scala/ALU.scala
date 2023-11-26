import chisel3._


class ALU extends Module {
    val io = IO(new Bundle {
        val in_operand_a = Input(SInt(32.W))
        val in_operand_b = Input(SInt(32.W))
        val in_aluOp = Input(UInt(3.W))
        val func7 = Input(UInt(1.W))
        val func3 = Input(UInt(3.W))
        val out_aluResult = Output(SInt(32.W))
    })

    val func = io.func7 ## io.func3

    when(io.in_aluOp === "b000".U) {             // r-type
        when(func === "b0000".U) {
            io.out_aluResult := io.in_operand_a + io.in_operand_b
        }.elsewhen(func === "b1000".U) {
            io.out_aluResult := io.in_operand_a - io.in_operand_b
        }.elsewhen(func === "b0100".U) {
            io.out_aluResult := io.in_operand_a ^ io.in_operand_b
        }.elsewhen(func === "b0001".U) {
            io.out_aluResult := io.in_operand_a << io.in_operand_b(4, 0)  // take the lower 5 bits
        }.elsewhen(func === "b0101".U || func === "b1101".U) {
            io.out_aluResult := io.in_operand_a >> io.in_operand_b(4, 0)
        }.elsewhen(func === "b0010".U) {
            when(io.in_operand_a.asSInt < io.in_operand_b.asSInt) {
                io.out_aluResult := 1.S
            }.otherwise {
                io.out_aluResult := 0.S
            }        
        }.elsewhen(func === "b0011".U) {
            when(io.in_operand_a.asUInt < io.in_operand_b.asUInt) {
                io.out_aluResult := 1.S
            }.otherwise {
                io.out_aluResult := 0.S
            }        
        }.elsewhen(func === "b0110".U) {
            io.out_aluResult := io.in_operand_a | io.in_operand_b
        }.elsewhen(func === "b0111".U) {
            io.out_aluResult := io.in_operand_a & io.in_operand_b
        }.elsewhen(func === "b0111".U) {
            io.out_aluResult := io.in_operand_a & io.in_operand_b
        }.otherwise {
            io.out_aluResult := 0.S
        }
    }.elsewhen(io.in_aluOp === "b100".U) {      // load-type
        io.out_aluResult := io.in_operand_a + io.in_operand_b  // load address
    }.elsewhen(io.in_aluOp === "b101".U) {      // s-type
        io.out_aluResult := io.in_operand_a + io.in_operand_b  // save address
    }.elsewhen(io.in_aluOp === "b010".U) {      // sb-type
        when(io.func3 === "b000".U) {              // beq 
            when(io.in_operand_a.asSInt === io.in_operand_b.asSInt) {
                io.out_aluResult := 1.S  // pc + imm
            }.otherwise {
                io.out_aluResult := 0.S
            }
        }.elsewhen(io.func3 === "b001".U) {           // bne
            when(io.in_operand_a.asSInt =/= io.in_operand_b.asSInt) {
                io.out_aluResult := 1.S  // pc + imm
            }.otherwise {
                io.out_aluResult := 0.S
            }
        }.elsewhen(io.func3 === "b100".U) {           // blt
            when(io.in_operand_a.asSInt < io.in_operand_b.asSInt) {
                io.out_aluResult := 1.S  // pc + imm
            }.otherwise {
                io.out_aluResult := 0.S
            }
        }.elsewhen(io.func3 === "b101".U) {           // bge
            when(io.in_operand_a.asSInt >= io.in_operand_b.asSInt) {
                io.out_aluResult := 1.S  // pc + imm
            }.otherwise {
                io.out_aluResult := 0.S
            }
        }.elsewhen(io.func3 === "b110".U) {           // bltu
            when(io.in_operand_a.asUInt < io.in_operand_b.asUInt) {
                io.out_aluResult := 1.S  // pc + imm
            }.otherwise {
                io.out_aluResult := 0.S
            }
        }.elsewhen(io.func3 === "b111".U) {           // bgeu
            when(io.in_operand_a.asUInt >= io.in_operand_b.asUInt) {
                io.out_aluResult := 1.S  // pc + imm
            }.otherwise {
                io.out_aluResult := 0.S
            }
        }.otherwise {
            io.out_aluResult := DontCare
        }
    }.elsewhen(io.in_aluOp === "b001".U) {      // i-type
        io.out_aluResult := io.in_operand_a + io.in_operand_b  // a + imm
    }.elsewhen(io.in_aluOp === "b011".U) {      // jalr/jal-type
        io.out_aluResult := io.in_operand_a.asSInt + io.in_operand_b  // pc + imm
    }.elsewhen(io.in_aluOp === "b110".U) {      // lui-type
        // io.out_aluResult := io.in_operand_a(19, 0) ## "h000".U
        io.out_aluResult := DontCare
    }.elsewhen(io.in_aluOp === "b111".U) {
        io.out_aluResult := DontCare
    }.otherwise {
        io.out_aluResult := DontCare
    }
}

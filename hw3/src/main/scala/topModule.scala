import chisel3._

class TopModule extends Module {
    val io = IO(new Bundle {
        val fetch_in_debug = Output(UInt(32.W))
        val reg_out1 = Output(SInt(32.W))
        val reg_out2 = Output(SInt(32.W))
 })
    val control = Module(new Control())
    val pc_counter = Module(new ProgramCounter())
    val imem = Module(new InstructionMemory(32))  // x0 ~ x31
    val imm_gen = Module(new ImmGenerator())
    val reg_file = Module(new RegisterFile())
    val dmem = Module(new DataMemory())
    val alu = Module(new ALU())

    imem.io.addr := pc_counter.io.pcOut.asUInt
    val instruction = imem.io.instrOut
    
    // Decode and control
    control.io.in_opcode := instruction(6, 0)
    imm_gen.io.in_instr := instruction

    // Register
    reg_file.io.in_rs1_sel := instruction(19, 15)
    reg_file.io.in_rs2_sel := instruction(24, 20)

    // ALU
    alu.io.in_aluOp := control.io.out_aluOp
    alu.io.func3 := instruction(14, 12)
    alu.io.func7 := instruction(31)
    // control operand a
    when(control.io.out_operand_a_sel === "b10".U){
        // jal-type
        alu.io.in_operand_a := pc_counter.io.pcOut
    }.elsewhen(control.io.out_operand_a_sel === "b01".U){
        // load-type, s-type
        alu.io.in_operand_a := instruction(19, 15).asSInt
    }.otherwise{
        // r-type, i-type, lui-type, sb-type
        alu.io.in_operand_a := reg_file.io.out_rs1
    }

    // control operand b
    when(control.io.out_operand_b_sel === "b1".U){
        when(control.io.out_extend_sel === "b00".U){
            // load-type
            alu.io.in_operand_b := imm_gen.io.out_i_imm
        }.elsewhen(control.io.out_extend_sel === "b01".U){
            // s-type
            alu.io.in_operand_b := imm_gen.io.out_s_imm
        }.elsewhen(control.io.out_extend_sel === "b10".U){
            // u-type
            alu.io.in_operand_b := imm_gen.io.out_u_imm
        }.elsewhen(control.io.out_extend_sel === "b11".U){
            // jal-type
            alu.io.in_operand_b := imm_gen.io.out_uj_imm
        }.otherwise{
            alu.io.in_operand_b := 0.S(32.W)
        }
    }.otherwise{
        // r-type, i-type, lui-type, sb-type
        alu.io.in_operand_b := reg_file.io.out_rs2
    }

    // Branching and Jumping
    when(control.io.out_next_pc_sel === "b10".U){
        // jal-type
        pc_counter.io.enableBranch := control.io.out_branch
        pc_counter.io.branchAddr := alu.io.out_aluResult
    }.elsewhen(control.io.out_next_pc_sel === "b11".U){
        // jalr-type
        pc_counter.io.enableBranch := control.io.out_branch
        pc_counter.io.branchAddr := alu.io.out_aluResult
    }.elsewhen(control.io.out_next_pc_sel === "b01".U){
        // branch-type
        when(alu.io.out_aluResult === 1.S){
            pc_counter.io.enableBranch := control.io.out_branch
            pc_counter.io.branchAddr := pc_counter.io.pcOut + imm_gen.io.out_sb_imm
        }.otherwise{
            pc_counter.io.enableBranch := 0.U
            pc_counter.io.branchAddr := DontCare
        }
    }.otherwise{
        // others
        pc_counter.io.enableBranch := 0.U
        pc_counter.io.branchAddr := DontCare
    }
    
    reg_file.io.in_regWrite := control.io.out_regWrite
    when(control.io.out_memWrite === 1.U){  // s-type
        dmem.io.in_wd_sel := 1.U
        dmem.io.in_rd_sel := 0.U
        dmem.io.in_dataAddr := alu.io.out_aluResult.asUInt
        dmem.io.in_data := reg_file.io.out_rs2
        reg_file.io.in_wdata := DontCare
        reg_file.io.in_waddr := DontCare
    }.elsewhen(control.io.out_memToReg === "b1".U){  // load-type
        dmem.io.in_wd_sel := 0.U
        dmem.io.in_rd_sel := 1.U
        dmem.io.in_dataAddr := alu.io.out_aluResult.asUInt
        dmem.io.in_data := DontCare
        reg_file.io.in_wdata := dmem.io.out_data
        reg_file.io.in_waddr := instruction(11, 7)
    }.otherwise{
        dmem.io.in_wd_sel := 0.U
        dmem.io.in_rd_sel := 0.U
        dmem.io.in_dataAddr := DontCare
        dmem.io.in_data := DontCare
        reg_file.io.in_wdata := alu.io.out_aluResult
        reg_file.io.in_waddr := instruction(11, 7)
    }

    io.fetch_in_debug := instruction
    io.reg_out1 := reg_file.io.out_a0
    io.reg_out2 := reg_file.io.out_a1
}

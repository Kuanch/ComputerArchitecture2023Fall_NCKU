import chisel3._

class ProgramCounter extends Module {
  val io = IO(new Bundle {
    val enableBranch = Input(Bool()) // Enable brench
    val branchAddr = Input(UInt(32.W)) // Brench address
    val pcOut = Output(UInt(32.W)) // 32-bit program counter output
  })

  // Initialize the register with a 32-bit width, starting value of 0
  val pcReg = RegInit(0.U(32.W))

  
  when(io.enableBranch) {   // If the branch is enabled, the program counter is set to the branch address
    pcReg := io.branchAddr
  }.otherwise {             // Increment the register by 4 at each clock cycle
    pcReg := pcReg + 4.U
  }

  // Output the value of the program counter
  io.pcOut := pcReg
}

import chisel3._
import chisel3.stage.ChiselStage

object Main extends App {
  // Generate the Verilog code
  (new ChiselStage).emitVerilog(new TopModule, args)
}

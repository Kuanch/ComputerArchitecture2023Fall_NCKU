import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataMemorySpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "DataMemory"

    it should "write and access right data" in {
        test (new DataMemory) { c =>
            c.io.in_dataAddr.poke(0.U)
            c.io.in_data.poke(0x12345678.S)
            c.io.in_wd_sel.poke(true.B)
            c.io.in_rd_sel.poke(false.B)
            c.clock.step(1)
            c.io.in_wd_sel.poke(false.B)
            c.io.in_rd_sel.poke(true.B)
            c.io.in_dataAddr.poke(0.U)
            c.clock.step(1)
            c.io.out_data.expect(0x12345678.S)
            c.clock.step(1)

            // overwrite
            c.io.in_dataAddr.poke(0.U)
            c.io.in_data.poke(5.S)
            c.io.in_wd_sel.poke(true.B)
            c.io.in_rd_sel.poke(false.B)
            c.clock.step(1)  // Perform the write operation
            c.io.in_wd_sel.poke(false.B)
            c.io.in_rd_sel.poke(true.B)
            c.io.in_dataAddr.poke(0.U)  // Address for the read operation
            c.clock.step(1)  // Read the new data
            c.io.out_data.expect(5.S)  // Check the new data
        }
    }
}
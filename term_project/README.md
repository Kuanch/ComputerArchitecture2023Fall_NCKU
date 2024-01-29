# Extent riscv-mini to comply with RV32M
> This is the term project of [NCKU Computer Architecture (Fall 2023)](https://wiki.csie.ncku.edu.tw/arch/schedule), contribured by [Kuanch](https://github.com/Kuanch/riscv-mini/tree/main).

## Summary
This project adapts [riscv-mini](https://github.com/ucb-bar/riscv-mini) to compatible with part of [RV32M](https://msyksphinz-self.github.io/riscv-isadoc/html/rvm.html), and a new 4 stages design.

Go to [Adaptions](#Adaptions) to see what is adapted, in summary
1. Support half-precision multiplication
2. Separate Memory stage from WriteBack stage, formed a 4 stages design
3. Because of the 4 stages adaption, add 2 new bypass to maintain the behaviors
4. TODO: try to make up failed tests, see [Debugging](#Debugging)

## Architecture
riscv-mini is a simple RISC-V 3-stage pipeline written in Chisel. Its datapath diagram is the following :

![diagram](https://hackmd.io/_uploads/B1qCaXZvT.png)


## Generate Waveform
riscv-mini provide tools to visualize the waveform, by
```
./VTile <hex file> [<vcd file> 2> <log file>]
```

Since we are working on RV32M, it is crucial to see the improvement on multiplication, by producing waveform of `./VTile tests/multiply.riscv.hex`, we could have the following waveform :

![image](https://hackmd.io/_uploads/r17jkV-vT.png)

It is also crucial to know its hierarchy shown in waveform before we begin :

![image](https://hackmd.io/_uploads/rkdVlNZwa.png)

## Customized C Program run on riscv-mini
riscv-mini use [riscv-tools](https://github.com/riscv-software-src/riscv-tools/tree/priv-1.7) to build the project, run
```bash=
export RISCV=<path to riscv tools for priv 1.7>
export PATH=$PATH:$RISCV/bin
./build-riscv-tools
```
to build the toolchain.

:::warning Bold text, Italic text, code and link :::
:warning: It's struggle to build the toolchain in modern develop enviroment, since it requires elder GCC version, use [gcc docker](https://hub.docker.com/_/gcc/tags?page=1&name=5.2) to build the toolchain, especially when it comes to generating the wareform.
:::

After building the toolchain, compile C program with
```bash
riscv32-unknown-elf-gcc -o main main.c
```

Dump the elf file with
```bash
riscv32-unknown-elf-objdump -d main
```
to have only RV32 instructions.

### Generate waveform of cusitmized C program
Modify `custom-bmark/Makefile` to comply with your C program
```bash
# under custom-bmark/
make
cd ..
make run-custom-bmark
```
to produce `.vcd` and have the number of cycle, you will see something like
`Simulation completed at time 2492 (cycle 249)` in `output/main.out`



## How multi-stage is designed in Chisel
In [assignment 3](https://hackmd.io/f0eqR4AWSNGRCze3Pa71DA), we look over a single cycle design, how the multi-stage is designed is still unclear, it's necessary to look into it before we really dive into the design.

The code below is all defined in `src/main/scala/mini/Datapath.scala`.

### pipeline registers
We could easily find the pipeline registers in the Datapath file, they are defined as following :
```c=45
  /** *** Fetch / Execute Registers ****
    */
  val fe_reg = RegInit(
    (new FetchExecutePipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U
    )
  )
```
and
```c=54
  /** *** Execute / Write Back Registers ****
    */
  val ew_reg = RegInit(
    (new ExecuteWritebackPipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U,
      _.alu -> 0.U,
      _.csr_in -> 0.U
    )
  )
```

The pipelining works with 
```c=100
  // Pipelining
  when(!stall) {
    fe_reg.pc := pc
    fe_reg.inst := inst
  }

  /** **** Execute ****
    */
  io.ctrl.inst := fe_reg.inst
```
and
```c=156
  }.elsewhen(!stall && !csr.io.expt) {
    ew_reg.pc := fe_reg.pc
    ew_reg.inst := fe_reg.inst
    ew_reg.alu := alu.io.out
    ew_reg.csr_in := Mux(io.ctrl.imm_sel === IMM_Z, immGen.io.out, rs1)
    st_type := io.ctrl.st_type
    ld_type := io.ctrl.ld_type
    wb_sel := io.ctrl.wb_sel
    wb_en := io.ctrl.wb_en
    csr_cmd := io.ctrl.csr_cmd
    illegal := io.ctrl.illegal
    pc_check := io.ctrl.pc_sel === PC_ALU
  }
```

Now it is clear where we can find the instructions between stages, that's important for our understanding and debugging.

### control logic
1. After fetching, the instruction to be execute is first input into control
```c=108
io.ctrl.inst := fe_reg.inst
```
&emsp;&emsp;the following behavior is defined in `src/main/scala/mini/Control.scala`, the Decode stage is also fused inside `Control.scala` like
```c=146
class Control extends Module {
  val io = IO(new ControlSignals)
  val ctrlSignals = ListLookup(io.inst, Control.default, Control.map)

  // Control signals for Fetch
  io.pc_sel := ctrlSignals(0)
  io.inst_kill := ctrlSignals(6).asBool

  // Control signals for Execute
  io.A_sel := ctrlSignals(1)
  io.B_sel := ctrlSignals(2)
  io.imm_sel := ctrlSignals(3)
  io.alu_op := ctrlSignals(4)
  io.br_type := ctrlSignals(5)
  io.st_type := ctrlSignals(7)

  // Control signals for Write Back
  io.ld_type := ctrlSignals(8)
  io.wb_sel := ctrlSignals(9)
  io.wb_en := ctrlSignals(10).asBool
  io.csr_cmd := ctrlSignals(11)
  io.illegal := ctrlSignals(12)
}

```
2. After the decoding, `A_sel`, `B_sel` and `alu_op` is input into ALU
```c=128
  // ALU operations
  alu.io.A := Mux(io.ctrl.A_sel === A_RS1, rs1, fe_reg.pc)
  alu.io.B := Mux(io.ctrl.B_sel === B_RS2, rs2, immGen.io.out)
  alu.io.alu_op := io.ctrl.alu_op
```

3. The result is then either input into WriteBack or DMEM

&emsp;&emsp;WriteBack
```c=159
ew_reg.alu := alu.io.out
```
&emsp;&emsp;DMem Accessing
```c=139
  val daddr = Mux(stall, ew_reg.alu, alu.io.sum) >> 2.U << 2.U
  val woffset = (alu.io.sum(1) << 4.U).asUInt | (alu.io.sum(0) << 3.U).asUInt
```


---

## Adaptions
If there is no specifications, the adaptions below pass all the tests by running
```scala
> test
```
under `sbt` environment, this is actually to be 2 parts

**1. unit test**
ALUTests, BrCondTests, ImmGenTests, CSRTests, CacheTests, DatapathTests

**2. integrated test**
CoreSimpleTests, CoreISATests, CoreBmarkTests(median, multiply, qsort, towers, vvadd)


### Extent RV32M by adding multiplier inside ALU
:::success Bold text, Italic text, code and link :::
**TL;DR** You could see all the commits at this [mul_in_alu](https://github.com/ucb-bar/riscv-mini/compare/main...Kuanch:riscv-mini:mul_in_alu) branch on Github.
:::

There are several places we need to take care for complying with RV32M:
**1. Instruction and Opcode**
&emsp;&emsp;Before starting, we need to define the format in `instructions.scala` like following:
```c=69
def MUL = BitPat("b0000001??????????000?????0110011")
```
&emsp;&emsp;and also in `Opcode.scala`, define `Funct3` and `Funct7` for `MUL`
```c
// Funct3
val MUL = BigInt("000", 2).U(3.W)
// Funct7
val MUL = BigInt("0000001", 2).U(7.W)
```

&emsp;&emsp;Which will be used to identify the control signal and testing later.


**2. Control**
&emsp;&emsp;riscv-mini define the behaviors of each kind of instructions in `Control.scala` in advance, the format is like the folloing, the meaning of each signal is tagged upon,

```c=72
//                                                   kill                       wb_en   illegal?
//  pc_sel  A_sel   B_sel  imm_sel   alu_op   br_type  |  st_type ld_type wb_sel  | csr_cmd |
//    |       |       |     |          |          |    |     |       |       |    |  |      |
List(PC_4  , A_XXX,  B_XXX, IMM_X, ALU_XXX   , BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, N, CSR.N, Y)
```

&emsp;&emsp;For `MUL`, we add
```c=125
MUL -> List(PC_4, A_RS1, B_RS2, IMM_X, ALU_MUL, BR_XXX, N, ST_XXX, LD_XXX, WB_ALU, Y, CSR.N, N)
```
&emsp;&emsp;its next PC is PC+4, RS2 is a register instead of an immediate, and there is not a load, store or csr, but it requires a writeback (wb_sel=Y).


**3. Alu**
&emsp;&emsp;The primary modification is at `AluArea`, by mapping `io.alu_op` in the Mux tree, it returns corresponding calculation:

```c=83
Mux(
    io.alu_op === ALU_MUL,
    mul,
    Mux(
      ...
)
```
&emsp;&emsp;Since it's 32-bits system, we implement a half-precision multiplier by
```c=76
val mul = io.A(width / 2 - 1, 0) * io.B(width / 2 - 1, 0)
```
&emsp;&emsp;this would take lower 16-bits to be multiplied, ensure the result fits within the 32-bits width.


**4. AluTest and TestUtils**
&emsp;&emsp;There 50 test cases already prepared in `src/test/scala/mini/TestUtils.scala`, the corresponding `Funct7`, `Funct3` and `Opcode` are determined in advance for testing the instructions, and registers are generated randomly.

&emsp;&emsp;We integrate `mul` test case like
```c=169
Cat(Funct7.MUL, rand_rs2, rand_rs1, Funct3.MUL, rand_rd, Opcode.RTYPE)
```

&emsp;&emsp;And in `src/test/scala/mini/ALUTests.scala`,
```c=32
val mul = VecInit(rs1.zip(rs2).map { case (a, b) => toBigInt((a & 0xFFFF) * (b & 0xFFFF)).U(xlen.W) })
```
&emsp;&emsp;is added to conduct a half-precision multiplication.


---

#### Run customized C program on the adapted design (mul supporting)
:::success Bold text, Italic text, code and link :::
**TL;DR** You could see all the mentioned files at this [commit](https://github.com/Kuanch/riscv-mini/commit/a69efffd5ca29f9339a71c5fbdf5e07fef900a42) on Github.
:::

In this section, we will verify the work above by giving C program, object file information and waveform respectively.

I write a simple C program for testing purpose like following
```c=
int mul(int a, int b) {
    int c = a * b;
    return c;
}

int main(int argc, char** argv) {
  int r = mul(3, 2);
  return 0;
}
```
&emsp;&emsp;Then we can compile them and dump elf and assembly it as hex file:
```c
// ... skip
000004d4 <main>:
     4d4:	fd010113          	addi	sp,sp,-48
     4d8:	02112623          	sw	ra,44(sp)
     4dc:	02812423          	sw	s0,40(sp)
     4e0:	03010413          	addi	s0,sp,48
     4e4:	fca42e23          	sw	a0,-36(s0)
     4e8:	fcb42c23          	sw	a1,-40(s0)
     4ec:	00200593          	li	a1,2
     4f0:	00300513          	li	a0,3
     4f4:	fa9ff0ef          	jal	49c <mul>
     4f8:	fea42623          	sw	a0,-20(s0)
     4fc:	00000793          	li	a5,0
     500:	00078513          	mv	a0,a5
     504:	02c12083          	lw	ra,44(sp)
     508:	02812403          	lw	s0,40(sp)
     50c:	03010113          	addi	sp,sp,48
     510:	00008067          	ret
// ... skip
0000049c <mul>:
     49c:	fd010113          	addi	sp,sp,-48
     4a0:	02812623          	sw	s0,44(sp)
     4a4:	03010413          	addi	s0,sp,48
     4a8:	fca42e23          	sw	a0,-36(s0)
     4ac:	fcb42c23          	sw	a1,-40(s0)
     4b0:	fdc42703          	lw	a4,-36(s0)
     4b4:	fd842783          	lw	a5,-40(s0)
     4b8:	02f707b3          	mul	a5,a4,a5
     4bc:	fef42623          	sw	a5,-20(s0)
     4c0:	fec42783          	lw	a5,-20(s0)
     4c4:	00078513          	mv	a0,a5
     4c8:	02c12403          	lw	s0,44(sp)
     4cc:	03010113          	addi	sp,sp,48
     4d0:	00008067          	ret
// ... skip
```
&emsp;&emsp;Finally, we can find `02f707b3` in .hex file
```
// ... skip
fd010113
10000073
11010113
07c12f83
fcb42c23
fca42e23
03010413
02812623
fef42623
02f707b3    // mul x15, x14, x15
fd842783
fdc42703
03010113
02c12403
00078513
fec42783
02812423
02112623
fd010113
00008067    // ret
// ... skip
```
&emsp;&emsp;this will be fetch by riscv-mini design, we can verify this by running verilator tool to produce its waveform.

![螢幕擷取畫面 2023-12-27 212513](https://hackmd.io/_uploads/rJuWIotP6.png)

&emsp;&emsp;It is clear to see `02f707b3` really being executed in the design.


### Cycle number after multiplier is designed
The following results is obtained by building elf with
```bash=
riscv32-unknown-elf-gcc ... -o main main.o syscalls.o crt.o -nostdlib -nostartfiles -lc -lgcc
elf2hex 16 32768 main > main.hex
VTile main.hex
```

as you could see the huge reduction of the number of cycle, and much slower cost growth :
|#cycle|  rv32i | support M |
|-| -------- | -------- |
|1 mul|   832   |   249   | 
|10 mul|  3470   |   342   |
|100 mul|   29931   |   1152   |

we use the following code to test "support M"
```c=
#include <stdint.h>
#define NUM_ITER 100

int mul32(int a, int b) {
    int c = a * b;
    return c;
}

int main(int argc, char** argv) {
  int32_t a = 1;
  int32_t b = 2;
  for (int i = 0; i < NUM_ITER; i++) {
    int32_t c = mul32(a, b + i);
  }

  return mul32(a, c);
}
```
and the code for "rv32i"
```c=
#include <stdint.h>

#define NUM_ITER 100

int32_t getbit(int32_t a, int32_t i)
{
    return (a >> i) & 1;
}

int32_t imul32(int32_t a, int32_t b)
{
    int32_t r = 0;
    for (int i = 0; i < 32; i++) {
        if (getbit(b, i))
            r += a;
        else
            a <<= 1;
    }
    return r;
}

int main()
{
    int32_t a = 1;
    int32_t b = 2;
    for (int i = 0; i < NUM_ITER; i++) {
        int32_t c = imul32(a, b + i);
    }

    return imul32(a, c);
}
```

---


### Adapt riscv-mini as 4 stages
:::success Bold text, Italic text, code and link :::
**TL;DR** You could see all the commits at this [4stages](https://github.com/Kuanch/riscv-mini/commits/4stages/src/main/scala/mini/Datapath.scala?author=Kuanch) branch on Github.
:::

:::danger Bold text, Italic text, code and link:::
:warning: This adpation pass all unit tests, but failed on part of the integrated tests, working on it. See [Debugging](#Debugging).
:::

In this section, I would like to seperate MEM stage, **since MEM+WB is the last efficient design in riscv-mini now**, especially when riscv-mini support RV32I only in original architecture.

For better understanding the architecture and smooth the path to 4 stage design, **I manually name some of the signals in the diagram with the variable name** :

![diagram](https://hackmd.io/_uploads/B14vFJ4up.png#center =500x)

&emsp;&emsp;you can find exact the corresponding variables in `Datapath.scala`, this will definitely help us to divide the stage.

**1. Pipeline Register**
The first thing is to separate registers for Memory and WriteBack
```c
  /** *** Execute / Memory Registers ****
    */
  val em_reg = RegInit(
    (new ExecuteMemoryPipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U,
      _.alu -> 0.U,
      _.csr_in -> 0.U
    )
  )

  /** *** Memory / Write Back Registers ****
    */
  val mw_reg = RegInit(
    (new MemoryWritebackPipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U,
      _.alu -> 0.U,
      _.csr_out -> 0.U,
      _.wdata -> 0.S
    )
  )
```
`em_reg` remains the same interface to `ew_reg` at this time, another alternative is moving CSR to EXE, like some of the [materials](https://www.reddit.com/r/RISCV/comments/177ll09/comment/k4z0754/?utm_source=share&utm_medium=web2x&context=3) suggested.

We create new pipeline register `mw_reg` for passing the writeback states, `mw_reg.alu` is simply receiving `em_reg.alu`, `mw_reg.wdata` is to receive reading data from memory, like `lw`, and since CSR instructions definitely write back to register file like `CSRRW`, `CSRRS`, or `CSRRC`, we pipeline the output to `mw_reg` from CSR.

And we pipelined it with
```c=179
  // Pipelining
  when(reset.asBool || !stall && csr.io.expt) {
    st_type := 0.U
    ld_type := 0.U
    m_wb_en := false.B
    w_wb_en := false.B
    csr_cmd := 0.U
    illegal := false.B
    pc_check := false.B
  }.elsewhen(!stall && !csr.io.expt) {
    em_reg.pc := fe_reg.pc
    em_reg.inst := fe_reg.inst
    em_reg.alu := alu.io.out
    em_reg.csr_in := Mux(io.ctrl.imm_sel === IMM_Z, immGen.io.out, rs1)
    mw_reg.inst := em_reg.inst
    mw_reg.pc := em_reg.pc
    mw_reg.alu := em_reg.alu
    mw_reg.csr_out := csr.io.out
    st_type := io.ctrl.st_type
    ld_type := io.ctrl.ld_type
    m_wb_sel := io.ctrl.wb_sel
    m_wb_en := io.ctrl.wb_en
    w_wb_sel := m_wb_sel
    w_wb_en := m_wb_en
    csr_cmd := io.ctrl.csr_cmd
    illegal := io.ctrl.illegal
    pc_check := io.ctrl.pc_sel === PC_ALU
  }
```

**2. WriteBack Registers**
There are 4 inputs into the Mux at WriteBack stage, I pipelined them into `mw_reg` and wire into corresponding ports
```c
  mw_reg.wdata := MuxLookup(ld_type, io.dcache.resp.bits.data.zext)(
    Seq(
      LD_LH -> lshift(15, 0).asSInt,
      LD_LB -> lshift(7, 0).asSInt,
      LD_LHU -> lshift(15, 0).zext,
      LD_LBU -> lshift(7, 0).zext
    )
  )
```
and the Mux at WB stage
```c=
val regWrite = 
    MuxLookup(w_wb_sel, mw_reg.alu.zext)(
        Seq(WB_MEM -> mw_reg.wdata, WB_PC4 -> (mw_reg.pc + 4.U).zext, WB_CSR -> mw_reg.csr_out.zext)
    ).asUInt
```

**3. Bypass**
```c
  // bypass
  val mem_rd_addr = em_reg.inst(11, 7)
  val mem_rs2_addr = em_reg.inst(24, 20)
  val mem_rs1hazard = m_wb_en && rs1_addr.orR && (rs1_addr === mem_rd_addr)
  val mem_rs2hazard = m_wb_en && rs2_addr.orR && (rs2_addr === mem_rd_addr)
  val wb_rd_addr = mw_reg.inst(11, 7)
  val wb_rs1hazard = w_wb_en && rs1_addr.orR && (rs1_addr === wb_rd_addr)
  val wb_rs2hazard = w_wb_en && rs2_addr.orR && (rs2_addr === wb_rd_addr)
  // bypass wb to ex
  val rs1 = Mux(m_wb_sel === WB_ALU && mem_rs1hazard, em_reg.alu, Mux(wb_rs1hazard, regWrite, regFile.io.rdata1))
  val rs2 = Mux(m_wb_sel === WB_ALU && mem_rs2hazard, em_reg.alu, Mux(wb_rs2hazard, regWrite, regFile.io.rdata2))
```
In the original 3 stages design, only `WB->EXE` bypass is design, for bypass `em_reg.alu` back to `rs1` and `rs2`. After seperate Memory stage from WB stage, new `WB->EXE` should be added.
1. `MEM->EXE` (`WB->EXE` in 3 stage design)
2. `WB->EXE` (new)

After the above adaptions, now this is how the design looks like:
![image](https://hackmd.io/_uploads/Hkqdaru_T.png =500x)

---

## Debugging
:::info :::
(1/8): pass more tests by correcting `wb_sel` when bypass
(1/7): calculations seems to be correct, although some CSR activate abnormal behaviors, the root cause remains unknown.
:::

**Latest Update: 1 / 8**
### Unit Test
| Test | Pass | PassItem |
| -------- | -------- | -------- |
| ALUTests     | :heavy_check_mark:      | all |
| BrCondTests  | :heavy_check_mark:      | all |
| ImmGenTests     | :heavy_check_mark:   | all |
| CSRTests     | :heavy_check_mark:      | all |
| CacheTests     | :heavy_check_mark:    | all |
| DatapathTests     | :heavy_check_mark: | all |

### Integrated Test
| Test | Pass | PassItem |
| -------- | -------- | -------- |
| CoreSimpleTests     | :heavy_check_mark: | all |
| CoreISATests     | :heavy_check_mark: | all |
| CoreBmarkTests     | :heavy_check_mark: | all |
| TileSimpleTests     | :heavy_check_mark:  | all |
| TileISATests     | :heavy_exclamation_mark:  | 33/41 |
| TileBmarkTests     | :heavy_exclamation_mark:  | 0/5 |
| TileLargeBmarkTests     | :heavy_exclamation_mark:  | 0/5 |

We failed on some of the integrated tests, but it is hard to debug since the highly integration of these tests, like **they are .hex and hard to read** ([issue: Test hexfile creation documentation](https://github.com/ucb-bar/riscv-mini/issues/54)), **unclear pass conditions** etc., we would like to take notes about the debugging progress.

### Why we have TOHOST=1337 signal when run into customized program

When we compile C program like we did at [Customized C Program run on riscv-mini](), it returns `TOHOST=1337` **even when we just have an empty main function**, we further examined .dump file and found the line

```c=
0000138c <handle_trap>:
    // skip
    13e0:	0af70c63          	beq	a4,a5,1498 <handle_trap+0x10c>
    13e4:	fdc42703          	lw	a4,-36(s0)
    13e8:	00800793          	li	a5,8
    13ec:	00f70863          	beq	a4,a5,13fc <handle_trap+0x70>
    13f0:	53900513          	li	a0,1337
    13f4:	f75ff0ef          	jal	1368 <tohost_exit>
    13f8:	0a00006f          	j	1498 <handle_trap+0x10c>
    13fc:	fd442783          	lw	a5,-44(s0)
    // skip
```
it is actually possible to trace why the exception code, interestingly, ChatGPT give another idea:
> Regarding the specific code "1337", without additional context, it's hard to determine its exact meaning. However, "1337" (or "leet" in leetspeak) is often used in programming and gaming cultures to signify expertise or to flag something as special or unusual. It's possible that this code is being used humorously or symbolically to indicate a unique or noteworthy state in the program.

We now track what is the meaning of 1337 code with the hex code.

```c
000012f0 <handle_trap>:
    12f0:	fd010113          	addi	sp,sp,-48
    12f4:	02112623          	sw	ra,44(sp)
    12f8:	02812423          	sw	s0,40(sp)
    12fc:	03010413          	addi	s0,sp,48
    1300:	fca42e23          	sw	a0,-36(s0)
    1304:	fcb42c23          	sw	a1,-40(s0)
    1308:	fcc42a23          	sw	a2,-44(s0)
    130c:	008007ef          	jal	a5,1314 <handle_trap+0x24>
    1310:	0c002573          	csrr	a0,stats
    1314:	fef42423          	sw	a5,-24(s0)
    1318:	fe042623          	sw	zero,-20(s0)
    131c:	fdc42703          	lw	a4,-36(s0)
    1320:	00200793          	li	a5,2
    1324:	02f71263          	bne	a4,a5,1348 <handle_trap+0x58>
    1328:	fd842783          	lw	a5,-40(s0)
    132c:	0007a703          	lw	a4,0(a5)
    1330:	fe842783          	lw	a5,-24(s0)
    1334:	0007a783          	lw	a5,0(a5)
    1338:	00f77733          	and	a4,a4,a5
    133c:	fe842783          	lw	a5,-24(s0)
    1340:	0007a783          	lw	a5,0(a5)
    1344:	0af70c63          	beq	a4,a5,13fc <handle_trap+0x10c>
    1348:	fdc42703          	lw	a4,-36(s0)
    134c:	00800793          	li	a5,8
    1350:	00f70863          	beq	a4,a5,1360 <handle_trap+0x70>
    1354:	53900513          	li	a0,1337
    1358:	f75ff0ef          	jal	12cc <tohost_exit>
    135c:	0a00006f          	j	13fc <handle_trap+0x10c>
    1360:	fd442783          	lw	a5,-44(s0)
    1364:	04478793          	addi	a5,a5,68
    1368:	0007a703          	lw	a4,0(a5)
    136c:	05d00793          	li	a5,93
```
by examining the code, we can write it like
```c=
a5 = x1310;
*(s0 - 6) = a5;
*(s0 - 5) = 0;
a4 = *(s0 - 36);
a5 = 2;
if (a4 != a5) goto x1348;
else {
    a4 = *(*(s0 - 40));
    a5 = *(*(s0 - 24));
    a4 &= a5;
    a5 = *(*(s0 - 24));
    if (a4 == a5) goto x13fc;
    else:
        // x1348
        a4 = *(s0 - 36);
        a5 = 8;
        if (a4 == a5) goto x1360;
        else {
            a0 = 1337;
            goto tohost_exit;
        }
}
```
or
```c=
a4 = *(*(s0 - 40));
a5 = *(*(s0 - 24));
if ((*(s0 - 36) != 2) & (*(s0 - 36) != 8) || ((a4 & a5) != a5) )
    a0 = 1337;
    goto tohost_exit;
```

We still couldn't figure out why the root of the exception code 1337 at this step.



### Examine the calculation results before and after the adaptions (deprecated)

~~Some of them (qsort.riscv.hex, customized program) can't end normally, by look over the waveforms, return TOHOST=1337.~~

~~We also investigate several simple tests like `rv32ui-p-addi.hex` and it seems okay for the calculations, but enter traps.~~
~~* rv32ui-p-addi.hex (TOHOST=668)~~
**3 stages**
![image](https://hackmd.io/_uploads/rJZbwkdda.png)
**4 stages**
![image](https://hackmd.io/_uploads/BkaWvJd_a.png)
**3 stages**
![image](https://hackmd.io/_uploads/rJY8vkOuT.png)
**4 stages**
![image](https://hackmd.io/_uploads/SkQuvyuOp.png)
**3 stages**
![image](https://hackmd.io/_uploads/HkE40yu_T.png)
**4 stages**
![image](https://hackmd.io/_uploads/B1lr01duT.png)

~~but these still resulting testing fails (TOHOST=668), surprisingly, when we run a complicated tests like `median.riscv-large.hex`, there is much lesser serious error (TOHOST=1), being determined as pass.~~


more inspections on [riscv-tests](https://github.com/riscv-software-src/riscv-tests) are needed, like how the tests are produced and the testing targets etc.

:::warning
Lack of the essential instruction coverage.
:::


### How riscv-tests works (updated at 1/27)
[riscv-tests](https://github.com/riscv-software-src/riscv-tests) is a repository hosting unit tests for RISC-V processors, 

Let's start from a really basic example, the following code define a isa test of `ADD`:
```c
// defined in riscv-tests/isa/macros/scalar/test_macros.h
// line 13
#define TEST_CASE( testnum, testreg, correctval, code... ) \
test_ ## testnum: \
    li  TESTNUM, testnum; \
    code; \
    li  x7, MASK_XLEN(correctval); \
    bne testreg, x7, fail;
// skip...
// line 131
#define TEST_RR_SRC1_EQ_DEST( testnum, inst, result, val1, val2 ) \
    TEST_CASE( testnum, x1, result, \
      li  x1, MASK_XLEN(val1); \
      li  x2, MASK_XLEN(val2); \
      inst x1, x1, x2; \
    )

// defined in riscv-tests/isa/rv64ui/add.S
TEST_RR_SRC1_EQ_DEST( 17, add, 24, 13, 11 );
```

With this kind of interfaces, it's ealier to cover similar instruction tests.

Let's dive into a more complicated test:




## Reference
### Toolchain building
[Error building binutils in riscv-gnu-toolchain target with GCC 4.9.4](https://github.com/riscv-software-src/riscv-tools/issues/228)
[RISC-V 初探 (building toolchain)](https://coldnew.github.io/c8717b7e/)
[riscv-gnu-toolchain工具链-从下载到运行 (!!CSDN!!) (building toolchain)](https://blog.csdn.net/limanjihe/article/details/122373942)
[Lab2: RISC-V RV32I[MACF] emulator with ELF support](https://hackmd.io/@sysprog/SJAR5XMmi)
### Design
[Where to put CSR unit in 5-stage pipeline ? (reddit)](https://www.reddit.com/r/RISCV/comments/177ll09/where_to_put_csr_unit_in_5stage_pipeline/)
### Troubleshoot after adaptions
[Test hexfile creation documentation](https://github.com/ucb-bar/riscv-mini/issues/54)
[Error information *** FAILED *** (tohost = 1337)](https://github.com/riscv-software-src/riscv-tests/issues/112)
[Using C++ Emulator fails when calling printf syscall from a RISC-V baremetal program](https://stackoverflow.com/questions/49771918/using-c-emulator-fails-when-calling-printf-syscall-from-a-risc-v-baremetal-pro)
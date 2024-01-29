# Single-cycle RISC-V CPU
Contributed by [Kuanch](https://github.com/Kuanch), mainly adapted from [lab3](https://github.com/Kuanch/ca2023-lab3), there are several contents from [hw3](https://github.com/Kuanch/ComputerArchitecture2023Fall_NCKU/tree/main/hw3).


## Hello World in Chisel
```scala
class Hello extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(1.W))
  })
  val CNT_MAX = (50000000 / 2 - 1).U;
  val cntReg  = RegInit(0.U(32.W))
  val blkReg  = RegInit(0.U(1.W))
  cntReg := cntReg + 1.U
  when(cntReg === CNT_MAX) {
    cntReg := 0.U
    blkReg := ~blkReg                                                                                                                                         
  }
  io.led := blkReg
}
```
There is no input port in this module, but only one input, the output constantly would signal `0` or `1`.

There is 2 memorizing registers `cntReg` and `blkReg`, `cntReg` periodically increasing and reset by `CNT_MAX`, `blkReg` would flip each time `cntReg` is reset.

So this is a Chisel design that drives an LED to blink at a regular interval. The design uses a counter `cntReg` to measure time and toggles the state of an LED `blkReg` after a specified number of clock cycles.



## Try to design it from scratch
I accidentally build a single cycle CPU prototype during running the tutorials all by myself, [here](https://github.com/Kuanch/ComputerArchitecture2023Fall_NCKU/tree/main/hw3) is the design and the testing covered part of it.

Which takes me some of the time and might be seems unhurried on the Assignment3 progress. 

## Complete Lab3 (WIP)
The hands-on is relatively simple since the controler and memory accessing part has been implemented. I would like to compare the crafted lab 3 design with my straightforward version.

1. Chisel Style Writing
    * Enumerated Object
    * Mux, MuxLookup

2. Design Difference
    * PC in InstructionFetch
    * ALU controler
    * aluop_source selection
    * Load Instructions


## Lab3 Testbench Waveform
### InstructionFetchTest
![image](https://hackmd.io/_uploads/r1bW7aWHT.png)

#### Observations
1. Instruction is `0x13` equal to `addi x0, x0, 0` (Why is it?)
2. Program Counter starts from `x10000` (since we have `val entry = 0x1000`), +4 when there is posedge.
3. When the jump_flag is pulled up (which is randomly input via `Random.nextInt(2)`), Program Counter is set back to `x10000`

### InstructionDecoderTest
![image](https://hackmd.io/_uploads/BJMgI6bHp.png)

#### Observations
##### clock 2
1. At clock 2, the instruction `0xa02223` poked, which is `sw x10, 4(x0)`.
2. Since it's a s-type, intended to write in to memory, `memory_write_enable` is pulled up
3. No registers need to be access, `reg_write_enable` remains no signal.
4. save `x10` value into `x0` with offset 4, so `regs_reg2_read_address` is `0`, `regs_reg2_read_address` is `0A` and `ex_immediate`  is `4`.
##### clock 3
5. At clock 3, the instruction `0x22B7` poked, which is `lui x5, 2`.
6. Since it's a u-type, neither `memory_read_enable` nor `memory_write_enable` enables.
7. `reg_write_enable` activates since `x5` should be overwrtied.
8. `ex_aluop1_source = 0` is used to write register `x5`, with `reg_write_address = 0x5`, and `ex_aluop2_source = 1` loads immediate `2`.
##### clock 4
9. At clock 4 the instruction `0x2081B3` is `add x3, x1, x2`.
10. Since it's a u-type, neither `memory_read_enable` nor `memory_write_enable` enables.
11. `reg_write_enable` activates since `x3` should be overwrtied.
12. `ex_aluop1_source = 0` is used to load register `x1` with `regs_reg1_read_address = 0x1`, and `ex_aluop2_source = 0` loads register `x2` with `regs_reg1_read_address = 0x2`.

### ExecuteTest
![image](https://hackmd.io/_uploads/rk_hQ0WHa.png)
#### Observations
1. Since the instruction `x3 = x2 + x1` poked, `alu.io.func` remains as `1`, means `ALUFunctions.add` in the first 100 cycles.
2. Then `beq x1, x2, 2` is poked, one `reg1_data` and `reg2_data` poked, since they are equal, `io.if_jump_flag` is set to 1, and since `io.instruction_address = 2` and `io.immediate = 2`, `io.jump_address = 2 + 2`
3. When `reg1_data` not equal to `reg2_data`, there is no jumping.

### CPUTest
#### ByteAccessTest
```C
.global _start
_start:
    li a0, 0x4
    li t0, 0xDEADBEEF     // lui x5, -136484 and addi x5, x5, -273
    sb t0, 0(a0)
    lw t1, 0(a0)
    li s2, 0x15
    sb s2, 1(a0)
    lw ra, 0(a0)
loop:
    j loop
```
![image](https://hackmd.io/_uploads/SycuNMGS6.png)

##### Observations
1. `li t0, 0xDEADBEEF` seems to be divided as 2 instructions, **because the immediate field is limited in size, and is obviously where a stall happened**.
2. For every `lw` instructions, `memory_read_enable` is activated, and for `sb`, `memory_write_enable` is activated.
3. At `lw t1, 0(a0)`, since `xDEADBEEF` is saved, and its first byte is `xEF`, so we could see `io_wb_reg_write_source` in id or `io_write_data` in register file is `xEF`.
4. Since the last line `j loop` jumping to its program counter constantly, `jump_flag_id` is set to be 1 till the end.

#### FibonacciTest
```c
static int fib(int a)
{
    if (a == 1 || a == 2)
        return 1;
    return fib(a - 1) + fib(a - 2);
}

int main()
{
    *((volatile int *) (4)) = fib(10);
    return 0;
}
```
![image](https://hackmd.io/_uploads/ryqn2k7rT.png)

##### Observations
1. `jump_flag_id` and `ex_aluop1_source` has a periodical pattern, when `ex_aluop1_source` is pulled up, the instruction always is `beq x14, x15, 16` and `bne x14, x15, 12` mapping to `if (a == 1 || a == 2)`, after `ex_aluop1_source`, `jump_flag_id` then is pulled up, jump to execute  `fib(a - 1) + fib(a - 2);`.
2. This pattern lasts 8 times since `fib(10)` is called, `fib(a - 1)` recursive call reaches the condition `a == 2`.
3. From 1419ps to 1435ps, its a series of operations to return back to `fib(a - 1) + fib(a - 2);`
4. After that, the restore and save return address actions are periodical, since `if (a == 1 || a == 2)` happened from time to time.
5. `beq x14, x15, 16` and `bne x14, x15, 12` happened afther the restore and save address actions.

## Run RISC-V assembly code in Assignment2 on My CPU
Here is the full C code for testing
```c
#include <stdio.h>
#include <stdint.h>


int32_t mmul(register int32_t a, register int32_t b) {
    register int32_t r = 0;
    a = a << 1; /* to counter last right shift */
    do {
        if((b & 1) != 0) {
            r = r + a;
        }
        b = b >> 1;
        r = r >> 1;
    } while(b != 0);
    return r;
}


/* float32 multiply */
int32_t fmul32(int32_t ia, int32_t ib) {
    /* define sign */
    int32_t sr = (ia ^ ib) >> 31;
    /* define mantissa */
    int32_t ma = (ia & 0x7fffff) | 0x800000;
    int32_t mb = (ib & 0x7fffff) | 0x800000;
    int32_t mr;
    /* define exponent */
    int32_t ea = ((ia >> 23) & 0xff);
    int32_t eb = ((ib >> 23) & 0xff);
    int32_t er;
    /* special values */
    if(ea == 0xff) {
        if(ma == 0x800000 && eb != 0) {
            return 0x7f800000 | sr << 31;
        }
        return 0x7f800001;
    }
    if(eb == 0xff) {
        if(mb == 0x800000 && ea != 0) {
            return 0x7f800000 | sr << 31;
        }
        return 0x7f800001;
    }
    if(ea == 0) {
        return sr << 31;
    }
    if(eb == 0) {
        return sr << 31;
    }
    /* multiplication */
    register int32_t mrtmp = mmul(ma, mb);
    register int32_t ertmp = ea + eb - 127;
    /* realign mantissa */
    register int32_t mshift = (mrtmp >> 24) & 1;
    mr = mrtmp >> mshift;
    er = ertmp + mshift;
    /* overflow and underflow */
    if(er <= 0) {
        return sr << 31;
    }
    if(er >= 0xff) {
        return 0x7f800000 | sr << 31;
    }
    /* result */
    return (sr << 31) | ((er & 0xff) << 23) | (mr & 0x7fffff);
}


int main() {
    float a = 1.65;
    float b = 2.5;
    int32_t ia = *(int32_t *) &a;
    int32_t ib = *(int32_t *) &b;
    *((volatile int *) (4)) = fmul32(ia, ib);

    a = 0;
    b = 2.5;
    ia = *(int32_t *) &a;
    ib = *(int32_t *) &b;
    *((volatile int *) (8)) = fmul32(ia, ib);

    a = 1.33;
    b = 4.2556;
    ia = *(int32_t *) &a;
    ib = *(int32_t *) &b;
    *((volatile int *) (12)) = fmul32(ia, ib);

    return 0;
}
```
and testbench added into CPUTest
```c
class Fmul32Test extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Single Cycle CPU")
  it should "floating multiplication" in {
    test(new TestTopModule("fmul32.asmbin")).withAnnotations(TestAnnotations.annos) { c =>
      for (i <- 1 to 50) {
        c.clock.step(1000)
        c.io.mem_debug_read_address.poke((i * 4).U) // Avoid timeout
      }
      c.io.mem_debug_read_address.poke(4.U)
      c.clock.step()
      c.io.mem_debug_read_data.expect("h4083ffff".U)

      c.io.mem_debug_read_address.poke(8.U)
      c.clock.step()
      c.io.mem_debug_read_data.expect(0.U)

      c.io.mem_debug_read_address.poke(12.U)
      c.clock.step(2)
      c.io.mem_debug_read_data.expect("h40b51e4b".U)
    }
  }
```

### Waveform

Since the waves are pretty complicated, let's translate it into RISC-V instructions, starting from the first jump into `fmul32(ia, ib)`:
```
DBDFF0EF    jal x1, -580, where the second jump_flag_id is pulled up
FB010113    addi x2, x2, -80
04112623    sw x1, 76(x2)
04812423    sw x8, 72(x2)
04912223    sw x9, 68(x2)
05212023    sw x18, 64(x2)
03312E23    sw x18, 64(x2)
05010413    addi x8, x2, 80
FAA42E23    sw x10, -68(x8)
FAB42C23    sw x11, -72(x8)
FBC42703    lw x14, -68(x8)
FB842783    lw x15, -72(x8)
00F747B3    xor x15, x14, x15
41F7D793    srai x15, x15, 31
FCF42E23    sw x15, -36(x8)
FBC42703    lw x14, -68(x8)
008007B7    lui x15, 2048
00F767B3    or x15, x14, x15
FCF42C23    sw x15, -40(x8)
FB842703    lw x14, -72(x8)
008007B7    lui x15, 2048
FFF78793    addi x15, x15, -1
00F77733    and x14, x14, x15
008007B7    lui x15, 2048
00F767B3    or x15, x14, x15
FCF42A23    sw x15, -44(x8)
FBC42783    lw x15, -68(x8)
4177D793    srai x15, x15, 23
0FF7F793    andi x15, x15, 255
FCF42823    sw x15, -48(x8)
FB842783    lw x15, -72(x8)
4177D793    srai x15, x15, 23
0FF7F793    andi x15, x15, 255
FCF42623    sw x15, -52(x8)
FD042703    lw x14, -48(x8)
...
```
The order of instructions seems to be different from the results from `riscv-none-elf-objdump -d fmul32.o`, but still we could see the operations are reasonable to original C code.

And also, **a lots of redundant `sw` and `lw` is involved in the instructions**, which is constant to what we observe in assignment 2.

**Let's again, optimize with `-O1` and analyze its waveform:**
```
DBDFF0EF    jal x1, -580
FB010113    addi x2, x2, -80
04112623    sw x1, 76(x2)
04812423    sw x8, 72(x2)
04912223    sw x9, 68(x2)
05212023    sw x18, 64(x2)
03312E23    sw x19, 60(x2)
05010413    addi x8, x2, 80
FAA42E23    sw x10, -68(x8)
FAB42C23    sw x11, -72(x8)
FBC42703    lw x14, -68(x8)
FB842783    lw x15, -72(x8)
00F747B3    xor x15, x14, x15
41F7D793    srai x15, x15, 31
FCF42E23    sw x15, -36(x8)
FBC42703    lw x14, -68(x8)
008007B7    lui x15, 2048
FFF78793    addi x15, x15, -1
00F77733    and x14, x14, x15
008007B7    lui x15, 2048
00F767B3    or x15, x14, x15
FCF42C23    sw x15, -40(x8)
FB842703    lw x14, -72(x8)
008007B7    lui x15, 2048
FFF78793    addi x15, x15, -1
00F77733    and x14, x14, x15
008007B7    lui x15, 2048
00F767B3    or x15, x14, x15
FCF42A23    sw x15, -44(x8)
FBC42783    lw x15, -68(x8)
4177D793    srai x15, x15, 23
0FF7F793    andi x15, x15, 255
FCF42823    sw x15, -48(x8)
FB842783    lw x15, -72(x8)
...
```
Even we see a more organized code in dump file, the code seems not to change much, but different orders. Here is the instructions from `-O2`:

```
DBDFF0EF
FB010113
04112623
04812423
04912223
05212023
03312E23
05010413
FAA42E23
FAB42C23
FBC42703
FB042783
00F747B3
41F7D793
FCF42E23
FBC42703
008007B7
FFF78793
00F77733
008007B7
00F767B3
FCF42C23
FB842703
008007B7
FFF78793
00F77733
008007B7
00F767B3
FCF42A23
FBC42783
4177D793    srai x15, x15, 23
0FF7F793    andi x15, x15, 255
FCF42823    sw x15, -48(x8)
FB842783    lw x15, -72(x8)
4177D793    srai x15, x15, 23
0FF7F793    andi x15, x15, 255
FCF42623    sw x15, -52(x8)
FD042703    lw x14, -48(x8)
...
```
Not much changes again, still only few registers involved and use `x14` and `x15` a lot. Let's dive into the waveform of `-O2` anyway.

![image](https://hackmd.io/_uploads/Sy_S8u4Sp.png)


:::spoiler Ask ChatGPT4
:question: I have a single-cycle cpu, I want to see the exact execution of instructions under different optimization level -O0, -O1 and -O2, but the instructions seems not too different except executed orders from the actual waveform, even though the "objdump -d" can tell the level of code organization is increasing, why is that?

Answer:
The actual execution of the instructions on a single-cycle CPU, as seen in a waveform, might not look drastically different because each instruction still takes one cycle to execute. However, the total number of instructions executed and the program's execution time may vary due to the optimizations.
:::


#### Observations
1. Like all other test, now I notice that all instructions are falling edge-triggered, and immediately followed by a rising edge-triggered `jump_address_id`, next instruction is then fetch at next rise edge, for example, `if(ea == 0xff)`, if it's staisfied, no jumping, else jump to `x1264`. 
It's okay to write like `if(ea == 0xff)` in a single-cycle CPU, but should we write `if(ea != 0xff)` if it's less likely to happen in pipelined CPU to have less stall?
2. The "`sw` followed by `lw`" pattern happens a lot, so it's symbolic that we can observe periodically `memory_write_enable` pulled up then `memory_read_enable`.
3. Jump because of`mmul(ma, mb);` is also eaily recognized, it equip a large value `ex_immediate`, after 4 times b-type jumping.

![圖片2](https://hackmd.io/_uploads/rJhff_EST.jpg)

4. There is a sequential b-type instructions after jumping to `mmul` because of `if((b & 1) != 0)` along with `srai`.
To be more specified, it actually repeats these instructions, mapping to exactly what C code acts:
```
andi x13, x15, 1
beq x13, x0, 8
srai x15, x15, 1
srai x9, x9, 1
bne x15, x0, -20
```

5. The first test case ends when `jalr x0, 0(x1)` happen, the `jump_flag_id` is pulled up, along with `ex_aluop1_source = 0` and `ex_aluop2_source = 1`.

6. Since the second test is `a = 0, b = 2.5`, it is return at `if(ea == 0xff)`, we can clearly see just short part wave for this case.

7. We can acutally tell 3 test cases with on the waveform, just find 3 j-type instuctions with `jump_address_id = x1194`, where `x1194` should be the address of function `fmul32`.

![圖片3](https://hackmd.io/_uploads/BymLK_NSp.jpg)



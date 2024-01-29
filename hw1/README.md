# Optimize the multiplication of 3 integer numbers

[TOC]

###### tags: `Computer Architecutre` `jserv` `NCKU`

## TL;DR
We implement a algorithm to multiply 3 numbers, faster than doing the common multiplication 2 times. **We will show our improved version with be more efficient comparing with the common multiplication, both of them focus on 32 bits integer, and without M extension.


## What and Why (Motivations)
Under the situation of no M extension supported, we achieve muliplication by check every bit of b and left-shift a each time. But in common, this need to iterate over each bit of b.

Even though we can early stop when b become zero (as we right-shift b after checking the bit), consider b is a large number (the 1 at the very left side of b), makes us might still need to look over most bits of b.

This problem become more serious when we need to multiply lots of numbers, in our case, 3 numbers multiplication

In the following sections, we show a way that **enumerate all the bit positions in b and c at once, and do a << (b_ibit+c_jbit)**, this could be faster than the common multilication without M extension.


## How we do this in common way
Consider the following integer multiplication in C:
```c
static inline int32_t getbit(int64_t value, int n)
{
    return (value >> n) & 1;
}

int32_t imul32(int32_t a, int32_t b)
{
    for (int i = 0; i < 32; i++) {
        if (getbit(b, i))
            r += a << i;
    }
    return r;
}
```
or faster, by right-shift b 

```c
int32_t imul32(int32_t a, int32_t b)
{
    while(b > 0)
    {
        if (b && 1)
        {
            r += a;
        }
        a = a << 1;
        b = b >> 1;
        
    }
}
```

If we have 3 number need to be multiplied, we need to call this function 2 times, like the following
```asm
data
a_val: .word 3
b_val: .word 4096
c_val: .word 4096

.text

main:
    lw a0, a_val
    lw a1, b_val
    lw a3, c_val

    li t0, 0
    call multiply

    li t0, 0
    mv a1, a3
    call multiply

    li a7, 1
    ecall
    li a7, 10
    ecall

multiply:
    # beg t0, t1, end_loop        # we don't want to iterate all the bits
    beq a1, zero, end_multiply    # unsigned and small number

    andi a2, a1, 1
    srli a1, a1, 1
    bnez a2, add_to_result
    slli a0, a0, 1
    j multiply

add_to_result:
    add t0, t0, a0
    slli a0, a0, 1
    j multiply

end_multiply:
    mv a0, t0
    ret
```

and note that, if the b and c are large, which means the bit position are quit left in the bits, leads `beq a1, zero, end_multiply` taking long time to proceed, and we need to do it 2 times in 3 numbers mutiplication.


## How does it work (Implementations)
So we actually want to avoid iterating lots of bits multiple times specifically, we will **scan the bits of 2 multiplicands at once**, and record all the bit position.

This would be faster than the common way when the multiplicands are large number(or high precision fixed-point number).

### 3 integer multiplication in asm
Intuitively, we do 
```c
int32_t imul32_3num(int32_t a, int32_t b, int32_t c)
{
    int32_t r = 0;
    for (int i = 0; i < 32; i++) {
        if (getbit(b, i)) {
            for (int j = 0; j < 32; j++) {
                if (getbit(c, j)) {
                    r += a << (i + j);
                }
            }
        }
    }
    return r;
}
```

for 3 numbers multiplication in bit-wise operation intuitively, this is appearly time-consuming, **we can scan both of bits of b and c one times**, and do the multiplications with recorded bit position later.

```c=
typedef struct {
    int *data;
    size_t size;
    size_t capacity;
} CVector;

void initArray(DynamicArray *arr, size_t initialCapacity) {
    arr->data = malloc(initialCapacity * sizeof(int));
    if (!arr->data) exit(1); // Memory allocation failed
    arr->size = 0;
    arr->capacity = initialCapacity;
}

void push_back(DynamicArray *arr, int value) {
    if (arr->size == arr->capacity) {
        // Double the capacity
        arr->capacity *= 2;
        arr->data = realloc(arr->data, arr->capacity * sizeof(int));
        if (!arr->data) exit(1); // Memory reallocation failed
    }
    arr->data[arr->size++] = value;
}

int32_t imul32_3num(DynamicArray *b_vec, DynamicArray *c_vec, int32_t a, int32_t b, int32_t c)
{
    int32_t r = 0;
    for (int i = 0; i < 32; i++) {   # scan b and c at same time
        if (getbit(b, i)) push_back(&b_vec, i);
        if (getbit(c, i)) push_back(&c_vec, i);
    }
    
    /* do r += a << (i + j) for all elements in b_vec and c_vec */
}
```

the asm code would be like
```asm
.data
a_val: .word 3
b_val: .word 4096
c_val: .word 4096

.text

main:
    lw a0, a_val
    lw a1, b_val
    lw a2, c_val
    addi sp, sp, -64
    mv t2, sp        # have stack base addr
    addi t3, t2, 32   # have stack base addr

scan_bc:
    or t0, a1, a2
    beq t0, zero, end_scan
    andi t0, a1, 1  # reuse t0
    andi t1, a2, 1

    srli a1, a1, 1  # reduce
    srli a2, a2, 1

    bnez t0, save_pos_counterB
    bnez t1, save_pos_counterC
    addi t6, t6, 1

    j scan_bc

save_pos_counterB:
    sw t6, 0(t2)
    addi t2, t2, 4
    addi t4, t4, 1
    bnez t1, save_pos_counterC
    addi t6, t6, 1
    j scan_bc

save_pos_counterC:
    sw t6, 0(t3)
    addi t3, t3, 4
    addi t5, t5, 1
    addi t6, t6, 1
    j scan_bc

end_scan:
    addi t2, t2, -4
    addi t3, t3, -4
    addi t6, t6, -1
    sw t3, 56(sp)
    sw t5, 60(sp)

loop_b_bit:
    beq t4, zero, end_multiply    # unsigned and small number
    addi t4, t4, -1

    # add highest bits of b and c
    lw t0, 0(t2)

loop_c_bit:
    # load next c bit
    addi t5, t5, -1

    lw t1, 0(t3)
    add t6, t0, t1
    sll t6, a0, t6
    add a1, a1, t6

    addi t3, t3, -4
    bnez t5, loop_c_bit
    
    # restore c info and to next bit of b
    lw t3, 56(sp)
    lw t5, 60(sp)

    addi t2, t2, -4
    j loop_b_bit

end_multiply:
    mv a0, a1
    addi sp, sp, 8
    li a7, 1
    ecall
    li a7, 10
    ecall

```
in this case, 4096's bit position is at 11th, this means we need to do 11 times `beq a1, zero, end_multiply` and their following code in `multiply`, and for b and c respectively.

In our implementation above, we only need to go through b and c one time (11), and when they are more sparse, the optimization is more efficient.


### Extend to fixed-point number
It might be rare to multiply 3 larger numbers in the real cases, but it would be valued if we express high precision float with fixed-point.

Consider a fixed-point float, say, 123456789 with scaling factor 1e-8, which represents 1.23456789 in decimal and 0b101111000 in binary (only 8bits), this case would make the approach mentioned above more pratical.


## Analysis
### Cycle
The most critical part is the cycles we used in our implementations like addressed above, with the common way to multiply 3 high bit position numbers the cycle is **257, under a=3, b=4096, c=4096**.

![](https://hackmd.io/_uploads/r1egGYzW6.png)


On the other hand, with our implementation, we can do the job on cycle **219, under the same input numbers**.

![](https://hackmd.io/_uploads/Bk7MMKzZa.png)


### Disassembled Executable Code

```asm
00000000 <main>:
    0:        10000517        auipc x10 0x10000
    4:        00052503        lw x10 0 x10
    8:        10000597        auipc x11 0x10000
    c:        ffc5a583        lw x11 -4 x11
    10:        10000617        auipc x12 0x10000
    14:        ff862603        lw x12 -8 x12
    18:        fc010113        addi x2 x2 -64
    1c:        00010393        addi x7 x2 0
    20:        02038e13        addi x28 x7 32

00000024 <scan_bc>:
    24:        00c5e2b3        or x5 x11 x12
    28:        04028863        beq x5 x0 80 <end_scan>
    2c:        0015f293        andi x5 x11 1
    30:        00167313        andi x6 x12 1
    34:        0015d593        srli x11 x11 1
    38:        00165613        srli x12 x12 1
    3c:        00029863        bne x5 x0 16 <save_pos_counterB>
    40:        02031263        bne x6 x0 36 <save_pos_counterC>
    44:        001f8f93        addi x31 x31 1
    48:        fddff06f        jal x0 -36 <scan_bc>

0000004c <save_pos_counterB>:
    4c:        01f3a023        sw x31 0 x7
    50:        00438393        addi x7 x7 4
    54:        001e8e93        addi x29 x29 1
    58:        00031663        bne x6 x0 12 <save_pos_counterC>
    5c:        001f8f93        addi x31 x31 1
    60:        fc5ff06f        jal x0 -60 <scan_bc>

00000064 <save_pos_counterC>:
    64:        01fe2023        sw x31 0 x28
    68:        004e0e13        addi x28 x28 4
    6c:        001f0f13        addi x30 x30 1
    70:        001f8f93        addi x31 x31 1
    74:        fb1ff06f        jal x0 -80 <scan_bc>

00000078 <end_scan>:
    78:        ffc38393        addi x7 x7 -4
    7c:        ffce0e13        addi x28 x28 -4
    80:        ffff8f93        addi x31 x31 -1
    84:        03c12c23        sw x28 56 x2
    88:        03e12e23        sw x30 60 x2

0000008c <loop_b_bit>:
    8c:        020e8c63        beq x29 x0 56 <end_multiply>
    90:        fffe8e93        addi x29 x29 -1
    94:        0003a283        lw x5 0 x7

00000098 <loop_c_bit>:
    98:        ffff0f13        addi x30 x30 -1
    9c:        000e2303        lw x6 0 x28
    a0:        00628fb3        add x31 x5 x6
    a4:        01f51fb3        sll x31 x10 x31
    a8:        01f585b3        add x11 x11 x31
    ac:        ffce0e13        addi x28 x28 -4
    b0:        fe0f14e3        bne x30 x0 -24 <loop_c_bit>
    b4:        03812e03        lw x28 56 x2
    b8:        03c12f03        lw x30 60 x2
    bc:        ffc38393        addi x7 x7 -4
    c0:        fcdff06f        jal x0 -52 <loop_b_bit>

000000c4 <end_multiply>:
    c4:        00058513        addi x10 x11 0
    c8:        00810113        addi x2 x2 8
    cc:        00100893        addi x17 x0 1
    d0:        00000073        ecall
    d4:        00a00893        addi x17 x0 10
    d8:        00000073        ecall
```

### 5-stage pipelined processor
I test the code using [Ripes](https://github.com/mortbopet/Ripes) simulator.

Pipeline Stage Explanation
Ripes provides different processors to run the code. And I choose 5-stage processor to run my program.

![](https://hackmd.io/_uploads/SyfLiFzZa.png)

I take the instruction `bne x5 x0 16 <save_pos_counterB>` for example and analyze how the processor operates the instruction in different stages.

According to [RISC-V Manual (p.17)](https://riscv.org//wp-content/uploads/2017/05/riscv-spec-v2.2.pdf):

![](https://hackmd.io/_uploads/BkychYMb6.png)

Let’s see how it go through each stage.
1. Instruction fetch (IF)

* Since the program has been executed for a while, the PC(Program Count) has came to 0x3c, the address of this instruction.
* Its machine code is 0x00029863
* The next sequential address is at 0x40, add 4 from the current address

![](https://hackmd.io/_uploads/SkuITYfWp.png)

2. ID
* At this stage, decoder decodes the instruction and reads the necessary registers from the register file, in this case, it reads the contents of x5(0x05) and x0(0x00), and also prepares `bne` instruction.
![](https://hackmd.io/_uploads/BJVzznz-6.png)


4. EX
* The `bne` makes the address of this instruction (0x3c) add 16
![](https://hackmd.io/_uploads/ryleV3MW6.png)

* the brench is active (taken), then PC is set to 0x4c, where is the address of `save_pos_counterB`, and the input port receive it from  EX
![](https://hackmd.io/_uploads/B1SYmnzZa.png)

4. MEM

* Since the branch prediction fails, IFID and IDEX send the clear signals at last cycle, makes them "nop" (creating bubbles) temporarily at this cycle
![](https://hackmd.io/_uploads/r1TmjoM-T.png)


5. WB
This stage is also not directly relevant for the bne instruction, as it doesn't produce a result that needs to be stored in a register. Like the MEM stage, it's part of the pipeline cycle but isn't used by this instruction.

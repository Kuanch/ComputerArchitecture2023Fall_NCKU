.text

.globl mmul32_s
.align 2

mmul32_s:
    addi     sp,  sp,  -20
    sw       ra,  0(sp)
    sw       s0,  4(sp)
    sw       s1,  8(sp)
    sw       t0,  12(sp)
    sw       t1,  16(sp)

    li       t0,  0            # r = #t0
    slli     a0,  a0,  1
    loop:
    andi     t1,  a1,  1
    beq      t1,  x0,  skip_add_a      # skip add if (b & 1) == 0
    add      t0,  t0,  a0
    skip_add_a:
    srli     a1,  a1,  1
    srli     t0,  t0,  1
    bne      a1,  x0,  loop     # loop back if b != 0
    mv       a0,  t0

    lw       ra,  0(sp)
    lw       s0,  4(sp)
    lw       s1,  8(sp)
    lw       t0,  12(sp)
    lw       t1,  16(sp)
    addi     sp,  sp,  20
    ret

.text

.globl mmul32_3num_s
.align 2

mmul32_3num_s:
    addi     sp,  sp,  -96
    sw       ra,  0(sp)
    sw       s0,  4(sp)
    sw       s1,  8(sp)
    sw       t0,  12(sp)
    sw       t1,  16(sp)

    addi     s0,  sp, 20
    addi     s1,  sp, 52
    li       t2,  23

    scan_bc:
        beqz t2, end_scan_bc
        andi t0, a1, 1
        andi t1, a2, 1

        srli a1, a1, 1
        srli a2, a2, 1

        bnez t0, save_pos_counterB
        bnez t1, save_pos_counterC
        addi t2, t2, -1                  # t2 = counter

        j scan_bc

    save_pos_counterB:
        sw t2, 0(s0)
        addi s0, s0, 4                 # s0 = &counterB
        addi t3, t3, 1                 # t3 = # of 1s in b
        bnez t1, save_pos_counterC
        addi t2, t2, -1
        j scan_bc

    save_pos_counterC:
        sw t2, 0(s1)
        addi s1, s1, 4                 # s1 = &counterC
        addi t4, t4, 1                 # t4 = # of 1s in c
        addi t2, t2, -1
        j scan_bc

    end_scan_bc:
        addi s0, s0, -4
        addi s1, s1, -4
        addi t2, t2, -1
        sw s1, 88(sp)
        sw t4, 92(sp)

    loop_b_bit:
        beqz t3, end_multiply
        addi t3, t3, -1

        lw t0, 0(s0)

    loop_c_bit:
        addi t4, t4, -1

        lw t1, 0(s1)
        add t2, t0, t1
        srl t2, a0, t2
        add t5, t5, t2
        srl t2, a0, t0
        add t5, t5, t2
        srl t2, a0, t1
        add t5, t5, t2

        addi s1, s1, -4
        bnez t4, loop_c_bit

        lw s1, 88(sp)
        lw t4, 92(sp)

        addi s0, s0, -4
        j loop_b_bit

    end_multiply:
        add       a0, a0, t5
        lw       ra,  0(sp)
        lw       s0,  4(sp)
        lw       s1,  8(sp)
        lw       t0,  12(sp)
        lw       t1,  16(sp)
        addi    sp,  sp,  96
        ret

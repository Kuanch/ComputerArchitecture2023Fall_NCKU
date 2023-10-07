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

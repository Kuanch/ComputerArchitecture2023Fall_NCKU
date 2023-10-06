.data
a_val: .word 3
b_val: .word 6
c_val: .word 10
c_addr: .word 0

.text

main:
    lw a0, a_val
    lw a1, b_val
    lw a2, c_val

    li t0, 0  # result
    li t1, 0  # counter
    la t2, c_addr
    li t5, 0  # counter2
    li t6, 0

scan_c:
    beq a2, zero, end_scan
    andi t3, a2, 1
    srli a2, a2, 1
    bnez t3, save_pos_counter
    addi t1, t1, 1
    j scan_c

save_pos_counter:
    sw t1, 0(t2)
    addi t2, t2, 4
    addi t1, t1, 1
    addi t6, t6, 1
    j scan_c

end_scan:
    addi t2, t2, -4
    addi sp, sp, -8
    sw t2, 0(sp)
    sw t6, 4(sp)

multiply3num:
    beq a1, zero, end_multiply    # unsigned and small number

    andi a2, a1, 1
    srli a1, a1, 1

    bnez a2, add_to_result
    addi t5, t5, 1
    j multiply3num

add_to_result:
    lw t3, 0(t2)
    addi t2, t2, -4
    beq t6, zero, skip_add
    addi t6, t6, -1

    add t3, t3, t5
    sll t4, a0, t3
    add t0, t0, t4
    j add_to_result

skip_add:
    lw t2, 0(sp)
    lw t6, 4(sp)
    addi t5, t5, 1
    j multiply3num

end_multiply:
    mv a0, t0
    addi sp, sp, 8
    li a7, 1
    ecall
    li a7, 10
    ecall

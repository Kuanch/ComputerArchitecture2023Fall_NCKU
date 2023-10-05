.data
a_val: .word 3
b_val: .word 6

.text

main:
    la a0, a_val
    la a1, b_val
    
    lw a0, 0(a0)
    lw a1, 0(a1)

    li t0, 0 
    li t1, 32
    li t2, 0

loop:
    # beg t0, t1, end_loop
    beq a1, zero, end_loop    # unsigned and small number

    andi a2, a1, 1
    bnez a2, add_to_result

continue:
    slli a0, a0, 1
    srli a1, a1, 1

    addi t0, t0, 1
    j loop

add_to_result:
    add t2, t2, a0
    j continue

end_loop:
    mv a0, t2
    li a7, 1
    ecall
    li a7, 10
    ecall

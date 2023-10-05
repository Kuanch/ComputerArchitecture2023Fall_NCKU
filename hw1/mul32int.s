.data
a_val: .word 3         # Sample value for a
b_val: .word 6         # Sample value for b

.text

main:
    # Load values of a and b
    la a0, a_val
    la a1, b_val
    
    lw a0, 0(a0)
    lw a1, 0(a1)

    li t0, 0           # i = 0 (loop counter)
    li t1, 32          # loop bound
    li t2, 0           # r = 0 (result, lower 32-bits)

loop:
    # Check loop condition
    bge t0, t1, end_loop

    andi a2, a1, 1
    beq a2, zero, continue

    add t2, t2, a0

continue:
    slli a0, a0, 1
    srli a1, a1, 1

    addi t0, t0, 1
    j loop


end_loop:
    mv a0, t2
    li a7, 1
    ecall
    li a7, 10
    ecall

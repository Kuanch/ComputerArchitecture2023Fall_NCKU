.data
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
    # beg t0, t1, end_loop
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
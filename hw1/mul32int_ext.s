.data
a_val: .word 3
b_val: .word 6

.text


# Function: multiply
# Arguments:
#   a: a0 (x10)
#   b: a1 (x11)
# Return:
#   a0 (x10)

# Function: main
main:
    la a0, a_val
    la a1, b_val
    
    lw a0, 0(a0)
    lw a1, 0(a1)
    call multiply        # call multiply function

    # Use syscall to print the value of c in Ripes console
    li a7, 1             # Specify syscall number 1 (print integer)
    ecall                # Invoke the syscall

    li a7, 10            # set return value of main to 0
    ecall                  # return

multiply:
    mul x10, x10, x11     # multiply a and b, result in a0 (x10)
    ret                   # return to caller

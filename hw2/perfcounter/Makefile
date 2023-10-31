.PHONY: clean

include ${RV32EMU}/mk/toolchain.mk

OPT = -Os
CFLAGS = -march=rv32i_zicsr_zifencei -mabi=ilp32 ${OPT} -Wall

OBJS = \
    getcycles.o \
    getinstret.o \
    mmul32.o \
	mmul32_3num.o \
    main.o
BIN = perfcount.elf

%.o: %.S
	$(CROSS_COMPILE)gcc $(CFLAGS) -c -o $@ $<

%.o: %.c
	$(CROSS_COMPILE)gcc $(CFLAGS) -c -o $@ $<

all: $(BIN)

$(BIN): $(OBJS)
	 $(CROSS_COMPILE)gcc -o $@ $^

clean:
	$(RM) $(BIN) $(OBJS)

.PHONY: clean

include ${RV32EMU}/mk/toolchain.mk

ASFLAGS = -march=rv32i -mabi=ilp32
LDFLAGS = --oformat=elf32-littleriscv

%.o: %.S
	$(CROSS_COMPILE)as -R $(ASFLAGS) -o $@ $<

all: fmul32.elf

fmul32.elf: fmul32.o
	$(CROSS_COMPILE)ld -o $@ $(LDFLAGS) $<

main: main.c
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -O0 -o main main.c && \
	riscv-none-elf-readelf -h main && echo "" && riscv-none-elf-size main && echo "" && rv32emu main

makes:
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -O0 -o main main.c && \
	riscv-none-elf-readelf -h fmul32 && echo "" && riscv-none-elf-size fmul32 && echo "" && rv32emu fmul32

makec:
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -O0 -o fmul32 fmul32.c && \
	riscv-none-elf-readelf -h fmul32 && echo "" && riscv-none-elf-size fmul32 && echo "" && rv32emu fmul32

makecO1:
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -O1 -o fmul32 fmul32.c && \
	riscv-none-elf-readelf -h fmul32 && echo "" && riscv-none-elf-size fmul32 && echo "" && rv32emu fmul32

makecO2:
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -O2 -o fmul32 fmul32.c && \
	riscv-none-elf-readelf -h fmul32 && echo "" && riscv-none-elf-size fmul32 && echo "" && rv32emu fmul32

makecO3:
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -O3 -o fmul32 fmul32.c && \
	riscv-none-elf-readelf -h fmul32 && echo "" && riscv-none-elf-size fmul32 && echo "" && rv32emu fmul32

makecOfast:
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -Ofast -o fmul32 fmul32.c && \
	riscv-none-elf-readelf -h fmul32 && echo "" && riscv-none-elf-size fmul32 && echo "" && rv32emu fmul32

makecOs:
	$(CROSS_COMPILE)gcc -march=rv32i -mabi=ilp32 -Os -o fmul32 fmul32.c && \
	riscv-none-elf-readelf -h fmul32 && echo "" && riscv-none-elf-size fmul32 && echo "" && rv32emu fmul32

clean:
	$(RM) fmul32.elf fmul32.o

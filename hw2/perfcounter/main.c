#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

extern uint64_t get_cycles();
extern uint64_t get_instret();

/*
 * Taken from the Sparkle-suite which is a collection of lightweight symmetric
 * cryptographic algorithms currently in the final round of the NIST
 * standardization effort.
 * See https://sparkle-lwc.github.io/
 */
extern int32_t mmul32_s(int32_t, int32_t);
extern int32_t mmul32_3num_s(int32_t, int32_t, int32_t);


int main(void)
{
    int32_t a = 0x3fc10000;
    int32_t b = 0x3fa12c00;
    int32_t c = 0x3f953000;
    int32_t ma = (a & 0x7fffff) | 0x800000;
    int32_t mb = (b & 0x7fffff) | 0x800000;
    int32_t mc = (c & 0x7fffff) | 0x800000;

    /* measure cycles */
    uint64_t instret = get_instret();
    uint64_t oldcount = get_cycles();
    int32_t r_3num = mmul32_3num_s(ma, mb, mc);
    uint64_t cyclecount = get_cycles() - oldcount;

    printf("cycle count: %u\n", (unsigned int) cyclecount);
    printf("instret: %x\n", (unsigned) (instret & 0xffffffff));

    ma = (a & 0x7fffff) | 0x800000;
    mb = (b & 0x7fffff) | 0x800000;
    mc = (c & 0x7fffff) | 0x800000;

    instret = get_instret();
    oldcount = get_cycles();
    int32_t temp = mmul32_s(ma, mb);
    int32_t r = mmul32_s(temp, mc);
    cyclecount = get_cycles() - oldcount;

    printf("cycle count: %u\n", (unsigned int) cyclecount);
    printf("instret: %x\n", (unsigned) (instret & 0xffffffff));

    assert(r == r_3num);

    return 0;
}

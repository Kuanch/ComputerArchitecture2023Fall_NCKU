# Matrix multiplication with floating point addition and multiplication
contributed by [Kuanch](https://github.com/Kuanch/ComputerArchitecture2023Fall_NCKU/tree/main/hw2).

This is inspired by the brilliant work of [kc71486](https://github.com/kc71486/Computer-Architecture/tree/main/hw1), with the [Hackmd document](https://github.com/kc71486/Computer-Architecture/tree/main/hw1).

## Motivation
Matrix multiplication is essential in the modern applications, and the work from kc71486 is clean and great implementation to achieve it with rv32i only.

Optimization the code is not the main purpose of this study, just try my best.



## C Code
The version 3 of kc71486's work already can seamless work on compiling with `-march=rv32i -mabi=ilp32` on rv32emu, I only add in testing code, refer to [here](https://github.com/Kuanch/ComputerArchitecture2023Fall_NCKU/blob/main/hw2/fmul32.c).

## Disassembly instructions
After disassembling the ELF file dumped by `riscv-none-elf-objdump -d`, we could have the following output.

Since the original code is lenthy and hard to read, let's **focus on the main part of this task, the muliplication of floating**, I reviewed all the asm code to add C code comment for understanding.

Consider the following C code:

```c
int32_t fmul32(int32_t ia, int32_t ib) {
    /* define sign */
    int32_t sr = (ia ^ ib) >> 31;
    /* define mantissa */
    int32_t ma = (ia & 0x7fffff) | 0x800000;
    int32_t mb = (ib & 0x7fffff) | 0x800000;
    int32_t mr;
    /* define exponent */
    int32_t ea = ((ia >> 23) & 0xff);
    int32_t eb = ((ib >> 23) & 0xff);
    int32_t er;
    /* special values */
    if(ea == 0xff) {
        if(ma == 0x800000 && eb != 0) {
            return 0x7f800000 | sr << 31;
        }
        return 0x7f800001;
    }
    if(eb == 0xff) {
        if(mb == 0x800000 && ea != 0) {
            return 0x7f800000 | sr << 31;
        }
        return 0x7f800001;
    }
    if(ea == 0) {
        return sr << 31;
    }
    if(eb == 0) {
        return sr << 31;
    }
    /* multiplication */
    register int32_t mrtmp = mmul(ma, mb);
    register int32_t ertmp = ea + eb - 127;
    /* realign mantissa */
    register int32_t mshift = (mrtmp >> 24) & 1;
    mr = mrtmp >> mshift;
    er = ertmp + mshift;
    /* overflow and underflow */
    if(er <= 0) {
        return sr << 31;
    }
    if(er >= 0xff) {
        return 0x7f800000 | sr << 31;
    }
    /* result */
    return (sr << 31) | ((er & 0xff) << 23) | (mr & 0x7fffff);
}
```

The following block is dumped by `riscv-none-elf-objdump -d` with different level of optimized flags, I add some comments for easier understanding.

Since the consideration of space, the complete code is shown here, only selected difference between flags would be presented in the following sections.

### `-O0`

* **Statistics**
    * LOC (Line Of Code): `433`
    * Allocate `144` bytes on stack
    * Branching and jump used: `101`
        * 90 Branching
        * 11 Jump
    * Number of registers used: `7` 
        * `ax` registers: `a0` `a1` `a2` `a3` `a4` `a5`
        * `sx` registers: `s0`
        * `tx` registers: None 
    * Number of `lw` and `sw`: `91`
        * 61 `lw`
        * 30 `sw`
* **Observations**
1. Lengthy code.
2. A lots of branching, since it's directly translated from C.
3. Access fewer registers.
4. Takes a lot time to save word and load word.

:::warning
:warning: Don't put the screenshots which contain plain text only. Instead, utilize HackMD syntax to annotate the text.
:notes: jserv
:::

:::success
Guan:
I correct this and put the total cycles and size information at [the end of this section](#Cycle-Count-from-perfcounter).
:::

```c
000103d0 <fmul32>:
   103d0:	f7010113          	add	sp,sp,-144
   103d4:	08112623          	sw	ra,140(sp)
   103d8:	08812423          	sw	s0,136(sp)
   103dc:	09010413          	add	s0,sp,144
   103e0:	f6a42e23          	sw	a0,-132(s0)  # a0 = a
   103e4:	f6b42c23          	sw	a1,-136(s0)  # a1 = b
   103e8:	f7c40793          	add	a5,s0,-132   
   103ec:	0007a783          	lw	a5,0(a5)
   103f0:	fef42623          	sw	a5,-20(s0)
   103f4:	f7840793          	add	a5,s0,-136
   103f8:	0007a783          	lw	a5,0(a5)
   103fc:	fef42423          	sw	a5,-24(s0)
   10400:	fec42783          	lw	a5,-20(s0)
   /* define sign */
   10404:	41f7d793          	sra	a5,a5,0x1f  # ia >> 31;
   10408:	fef42223          	sw	a5,-28(s0)
   1040c:	fe842783          	lw	a5,-24(s0)
   10410:	41f7d793          	sra	a5,a5,0x1f  # ib >> 31;
   10414:	fef42023          	sw	a5,-32(s0)
   10418:	fec42703          	lw	a4,-20(s0)
   /* define mantissa */
   1041c:	008007b7          	lui	a5,0x800
   10420:	fff78793          	add	a5,a5,-1 # 7fffff <__BSS_END__+0x7db0ef>
   10424:	00f77733          	and	a4,a4,a5    # (ia & 0x7FFFFF)
   10428:	008007b7          	lui	a5,0x800
   1042c:	00f767b3          	or	a5,a4,a5    # | 0x800000;
   10430:	fcf42e23          	sw	a5,-36(s0)
   10434:	fe842703          	lw	a4,-24(s0)
   10438:	008007b7          	lui	a5,0x800
   1043c:	fff78793          	add	a5,a5,-1 # 7fffff <__BSS_END__+0x7db0ef>
   10440:	00f77733          	and	a4,a4,a5    # (ib & 0x7FFFFF)
   10444:	008007b7          	lui	a5,0x800
   10448:	00f767b3          	or	a5,a4,a5    # | 0x800000;
   1044c:	fcf42c23          	sw	a5,-40(s0)
   10450:	fec42783          	lw	a5,-20(s0)
   /* define exponent */
   10454:	4177d793          	sra	a5,a5,0x17  # (ia >> 23)
   10458:	0ff7f793          	zext.b	a5,a5       # & 0xFF
   1045c:	fcf42a23          	sw	a5,-44(s0)
   10460:	fe842783          	lw	a5,-24(s0)
   10464:	4177d793          	sra	a5,a5,0x17  # (ib >> 23)
   10468:	0ff7f793          	zext.b	a5,a5       # & 0xFF
   1046c:	fcf42823          	sw	a5,-48(s0)
   10470:	fd442703          	lw	a4,-44(s0)
   # if(ea == 0xFF && ma != 0x800000) ...
   10474:	0ff00793          	li	a5,255
   10478:	02f71463          	bne	a4,a5,104a0 <fmul32+0xd0>
   1047c:	fdc42703          	lw	a4,-36(s0)
   10480:	008007b7          	lui	a5,0x800
   10484:	00f70e63          	beq	a4,a5,104a0 <fmul32+0xd0>
   10488:	7ff807b7          	lui	a5,0x7ff80
   1048c:	00178793          	add	a5,a5,1 # 7ff80001 <__BSS_END__+0x7ff5b0f1>
   10490:	faf42423          	sw	a5,-88(s0)
   10494:	fa840793          	add	a5,s0,-88
   10498:	0007a783          	lw	a5,0(a5)
   1049c:	2540006f          	j	106f0 <fmul32+0x320>
   # if(eb == 0xFF && mb != 0x800000) ...
   104a0:	fd042703          	lw	a4,-48(s0)
   104a4:	0ff00793          	li	a5,255
   104a8:	02f71463          	bne	a4,a5,104d0 <fmul32+0x100>
   104ac:	fd842703          	lw	a4,-40(s0)
   104b0:	008007b7          	lui	a5,0x800
   104b4:	00f70e63          	beq	a4,a5,104d0 <fmul32+0x100>
   104b8:	7ff807b7          	lui	a5,0x7ff80
   104bc:	00178793          	add	a5,a5,1 # 7ff80001 <__BSS_END__+0x7ff5b0f1>
   104c0:	faf42223          	sw	a5,-92(s0)
   104c4:	fa440793          	add	a5,s0,-92
   104c8:	0007a783          	lw	a5,0(a5)
   104cc:	2240006f          	j	106f0 <fmul32+0x320>
   # if(ea == 0xFF && ma == 0x800000) ...
   104d0:	fd442703          	lw	a4,-44(s0)
   104d4:	0ff00793          	li	a5,255
   104d8:	04f71c63          	bne	a4,a5,10530 <fmul32+0x160>
   104dc:	fdc42703          	lw	a4,-36(s0)
   104e0:	008007b7          	lui	a5,0x800
   104e4:	04f71663          	bne	a4,a5,10530 <fmul32+0x160>
   104e8:	fd042783          	lw	a5,-48(s0)
   104ec:	00079e63          	bnez	a5,10508 <fmul32+0x138>
   104f0:	7f8007b7          	lui	a5,0x7f800
   104f4:	00178793          	add	a5,a5,1 # 7f800001 <__BSS_END__+0x7f7db0f1>
   104f8:	faf42023          	sw	a5,-96(s0)
   104fc:	fa040793          	add	a5,s0,-96
   10500:	0007a783          	lw	a5,0(a5)
   10504:	1ec0006f          	j	106f0 <fmul32+0x320>
   10508:	fe442703          	lw	a4,-28(s0)
   1050c:	fe042783          	lw	a5,-32(s0)
   10510:	00f747b3          	xor	a5,a4,a5
   10514:	01f79713          	sll	a4,a5,0x1f
   10518:	7f8007b7          	lui	a5,0x7f800
   1051c:	00f767b3          	or	a5,a4,a5
   10520:	f8f42e23          	sw	a5,-100(s0)
   10524:	f9c40793          	add	a5,s0,-100
   10528:	0007a783          	lw	a5,0(a5) # 7f800000 <__BSS_END__+0x7f7db0f0>
   1052c:	1c40006f          	j	106f0 <fmul32+0x320>
   # if(eb == 0xFF && mb == 0x800000) ...
   10530:	fd042703          	lw	a4,-48(s0)
   10534:	0ff00793          	li	a5,255
   10538:	04f71c63          	bne	a4,a5,10590 <fmul32+0x1c0>
   1053c:	fd842703          	lw	a4,-40(s0)
   10540:	008007b7          	lui	a5,0x800
   10544:	04f71663          	bne	a4,a5,10590 <fmul32+0x1c0>
   10548:	fd442783          	lw	a5,-44(s0)
   1054c:	00079e63          	bnez	a5,10568 <fmul32+0x198>
   10550:	7f8007b7          	lui	a5,0x7f800
   10554:	00178793          	add	a5,a5,1 # 7f800001 <__BSS_END__+0x7f7db0f1>
   10558:	f8f42c23          	sw	a5,-104(s0)
   1055c:	f9840793          	add	a5,s0,-104
   10560:	0007a783          	lw	a5,0(a5)
   10564:	18c0006f          	j	106f0 <fmul32+0x320>
   10568:	fe442703          	lw	a4,-28(s0)
   1056c:	fe042783          	lw	a5,-32(s0)
   10570:	00f747b3          	xor	a5,a4,a5
   10574:	01f79713          	sll	a4,a5,0x1f
   10578:	7f8007b7          	lui	a5,0x7f800
   1057c:	00f767b3          	or	a5,a4,a5
   10580:	f8f42a23          	sw	a5,-108(s0)
   10584:	f9440793          	add	a5,s0,-108
   10588:	0007a783          	lw	a5,0(a5) # 7f800000 <__BSS_END__+0x7f7db0f0>
   1058c:	1640006f          	j	106f0 <fmul32+0x320>
   # if(ea == 0 || eb == 0) ...
   10590:	fd442783          	lw	a5,-44(s0)
   10594:	00078663          	beqz	a5,105a0 <fmul32+0x1d0>
   10598:	fd042783          	lw	a5,-48(s0)
   1059c:	02079263          	bnez	a5,105c0 <fmul32+0x1f0>
   105a0:	fe442703          	lw	a4,-28(s0)
   105a4:	fe042783          	lw	a5,-32(s0)
   105a8:	00f747b3          	xor	a5,a4,a5
   105ac:	01f79793          	sll	a5,a5,0x1f
   105b0:	f8f42823          	sw	a5,-112(s0)
   105b4:	f9040793          	add	a5,s0,-112
   105b8:	0007a783          	lw	a5,0(a5)
   105bc:	1340006f          	j	106f0 <fmul32+0x320>
   105c0:	fe442703          	lw	a4,-28(s0)
   105c4:	fe042783          	lw	a5,-32(s0)
   /* multiplication */
   105c8:	00f747b3          	xor	a5,a4,a5       #  sr = sa ^ sb;
   105cc:	fcf42623          	sw	a5,-52(s0)
   105d0:	fd842583          	lw	a1,-40(s0)
   105d4:	fdc42503          	lw	a0,-36(s0)
   105d8:	cc9ff0ef          	jal	102a0 <imul32> # imul32(ma, mb)
   105dc:	00050713          	mv	a4,a0
   105e0:	00058793          	mv	a5,a1
   105e4:	fae42623          	sw	a4,-84(s0)
   105e8:	faf42823          	sw	a5,-80(s0)
   105ec:	fb042783          	lw	a5,-80(s0)
   105f0:	0177d793          	srl	a5,a5,0x17     # >> 23
   105f4:	00078713          	mv	a4,a5
   105f8:	fac42783          	lw	a5,-84(s0)
   105fc:	00979793          	sll	a5,a5,0x9
   10600:	00f767b3          	or	a5,a4,a5
   10604:	fcf42423          	sw	a5,-56(s0)
   10608:	fd442703          	lw	a4,-44(s0)
   1060c:	fd042783          	lw	a5,-48(s0)
   10610:	00f707b3          	add	a5,a4,a5       # ea + eb
   10614:	f8178793          	add	a5,a5,-127     # - 127
   10618:	fcf42223          	sw	a5,-60(s0)
   1061c:	fc842783          	lw	a5,-56(s0)
   10620:	4187d793          	sra	a5,a5,0x18     # getbit(mrtmp, 24)
   10624:	0017f793          	and	a5,a5,1        # & 1;
   10628:	fcf42023          	sw	a5,-64(s0)
   1062c:	fc042783          	lw	a5,-64(s0)
   10630:	fc842703          	lw	a4,-56(s0)
   10634:	40f757b3          	sra	a5,a4,a5       # mr = mrtmp >> mshift;
   10638:	faf42e23          	sw	a5,-68(s0)
   1063c:	fc042783          	lw	a5,-64(s0)
   # er = mshift ? inc(ertmp) : ertmp;
   10640:	00078863          	beqz	a5,10650 <fmul32+0x280>
   10644:	fc442783          	lw	a5,-60(s0)
   10648:	00178793          	add	a5,a5,1
   1064c:	0080006f          	j	10654 <fmul32+0x284>
   10650:	fc442783          	lw	a5,-60(s0)
   # if(er < 0) ...
   10654:	faf42c23          	sw	a5,-72(s0)
   10658:	fb842783          	lw	a5,-72(s0)
   1065c:	0207d263          	bgez	a5,10680 <fmul32+0x2b0>
   10660:	fe442703          	lw	a4,-28(s0)
   10664:	fe042783          	lw	a5,-32(s0)
   10668:	00f747b3          	xor	a5,a4,a5      # (sa ^ sb)
   1066c:	01f79793          	sll	a5,a5,0x1f    # << 31
   10670:	f8f42623          	sw	a5,-116(s0)
   10674:	f8c40793          	add	a5,s0,-116
   10678:	0007a783          	lw	a5,0(a5)
   1067c:	0740006f          	j	106f0 <fmul32+0x320>
   # if(er >= 0xFF) ...
   10680:	fb842703          	lw	a4,-72(s0)
   10684:	0fe00793          	li	a5,254
   10688:	02e7d663          	bge	a5,a4,106b4 <fmul32+0x2e4>
   1068c:	fe442703          	lw	a4,-28(s0)
   10690:	fe042783          	lw	a5,-32(s0)
   10694:	00f747b3          	xor	a5,a4,a5     # (sa ^ sb) << 31
   10698:	01f79713          	sll	a4,a5,0x1f
   1069c:	7f8007b7          	lui	a5,0x7f800
   106a0:	00f767b3          	or	a5,a4,a5     # 0x7F800000 |
   106a4:	f8f42423          	sw	a5,-120(s0)
   106a8:	f8840793          	add	a5,s0,-120
   106ac:	0007a783          	lw	a5,0(a5) # 7f800000 <__BSS_END__+0x7f7db0f0>
   106b0:	0400006f          	j	106f0 <fmul32+0x320>
   # result = (sr << 31) | ((er & 0xFF) << 23) | (mr & 0x7FFFFF);
   106b4:	fcc42783          	lw	a5,-52(s0)
   106b8:	01f79713          	sll	a4,a5,0x1f    # << 31
   106bc:	fb842783          	lw	a5,-72(s0)
   106c0:	01779693          	sll	a3,a5,0x17    # << 23
   106c4:	7f8007b7          	lui	a5,0x7f800
   106c8:	00f6f7b3          	and	a5,a3,a5      # (er & 0xFF)
   106cc:	00f76733          	or	a4,a4,a5      # (sr << 31) | ((er & 0xFF) << 23)
   106d0:	fbc42683          	lw	a3,-68(s0)
   106d4:	008007b7          	lui	a5,0x800
   106d8:	fff78793          	add	a5,a5,-1 # 7fffff <__BSS_END__+0x7db0ef>
   106dc:	00f6f7b3          	and	a5,a3,a5      # (mr & 0x7FFFFF)
   106e0:	00f767b3          	or	a5,a4,a5
   106e4:	faf42a23          	sw	a5,-76(s0)
   106e8:	fb440793          	add	a5,s0,-76
   106ec:	0007a783          	lw	a5,0(a5)
   return *(float *) &result;
   106f0:	00078513          	mv	a0,a5
   106f4:	08c12083          	lw	ra,140(sp)
   106f8:	08812403          	lw	s0,136(sp)
   106fc:	09010113          	add	sp,sp,144
   10700:	00008067          	ret
```

### -O1

* **Statistics**
    * LOC (Line Of Code): `94`
    * Allocate `32` bytes on stack
    * Branching and jump used: `55`
        * 44 Branching
        * 11 Jump
    * Number of registers used: `10` 
        * `ax` registers: `a0` `a1` `a2` `a3` `a4` `a5` `a6`
        * `sx` registers: `s0` `s1` `s2`
        * `tx` registers: None 
    * Number of `lw` and `sw`: `14`
        * 10 `lw`
        * 4 `sw`

* **Observations**
1. All statistics improved compared with -O0.
2. Less saving word operation, just see the `sw	a0,-132(s0)` and `sw	a1,-136(s0)` at the beginning of -O0, -O1 avoid to do that.
3. More registers are re-used, to achieve less saving operation.
4. More intelligent branching, in -O0 the code is really straightforward, but -O1 seperate the code, like special values and overflow-underflow checking into 2 part, if the simple condition is not satisfied, the code is discarded.
5. From 1. and 3., the code is more compact, smaller size, means less cycles and less cache footprint.


```c
0001027c <fmul32>:
   1027c:	fe010113          	add	sp,sp,-32
   10280:	00112e23          	sw	ra,28(sp)
   10284:	00812c23          	sw	s0,24(sp)
   10288:	00912a23          	sw	s1,20(sp)
   /* define sign */
   1028c:	41f55713          	sra	a4,a0,0x1f       # ia >> 31;
   10290:	41f5d813          	sra	a6,a1,0x1f       # ib >> 31;
   /* define mantissa */
   10294:	008007b7          	lui	a5,0x800
   10298:	fff78793          	add	a5,a5,-1 # 7fffff <__BSS_END__+0x7db0a7>
   1029c:	00a7f633          	and	a2,a5,a0
   102a0:	00b7f7b3          	and	a5,a5,a1
   /* define exponent */
   102a4:	41755513          	sra	a0,a0,0x17       # ia >> 23;
   102a8:	0ff57493          	zext.b	s1,a0
   102ac:	4175d593          	sra	a1,a1,0x17       # ib >> 23;
   102b0:	0ff5f413          	zext.b	s0,a1
   # special values 1
   102b4:	0ff00693          	li	a3,255
   102b8:	06d48e63          	beq	s1,a3,10334 <fmul32+0xb8>  # if(ea == 0xff)
   102bc:	0ff00693          	li	a3,255
   102c0:	0ad40063          	beq	s0,a3,10360 <fmul32+0xe4>  # if(eb == 0xff)
   102c4:	0c048063          	beqz	s1,10384 <fmul32+0x108>  # if(ea == 0)
   102c8:	0a040e63          	beqz	s0,10384 <fmul32+0x108> # if(eb == 0)
   102cc:	01212823          	sw	s2,16(sp)
   # multiplication
   102d0:	01074933          	xor	s2,a4,a6
   102d4:	00800537          	lui	a0,0x800
   102d8:	00a7e5b3          	or	a1,a5,a0
   102dc:	00a66533          	or	a0,a2,a0
   102e0:	f21ff0ef          	jal	10200 <imul32>  # imul32(ma, mb);
   102e4:	0175d793          	srl	a5,a1,0x17      # mr.l >> 23
   102e8:	00951513          	sll	a0,a0,0x9       # (jmr.h << 9)
   102ec:	00a7e7b3          	or	a5,a5,a0
   102f0:	00848433          	add	s0,s1,s0
   102f4:	4187d713          	sra	a4,a5,0x18      # (mrtmp >> 24)
   102f8:	00177693          	and	a3,a4,1         #  & 1;
   102fc:	00868733          	add	a4,a3,s0        # er = ertmp + mshift;
   10300:	f8170713          	add	a4,a4,-127      # ea + eb - 127;
   # overflow and underflow 1
   10304:	08074e63          	bltz	a4,103a0 <fmul32+0x124>   # if(er < 0) ...
   10308:	0fe00613          	li	a2,254
   1030c:	0ae64063          	blt	a2,a4,103ac <fmul32+0x130>    # if(er >= 0xFF) ...
   10310:	40d7d7b3          	sra	a5,a5,a3        # mr = mrtmp >> mshift;
   10314:	00979793          	sll	a5,a5,0x9       # 
   10318:	0097d793          	srl	a5,a5,0x9       # (mr & 0x7FFFFF);
   1031c:	01f91513          	sll	a0,s2,0x1f      # (sr << 31)
   10320:	00a7e7b3          	or	a5,a5,a0
   10324:	01771713          	sll	a4,a4,0x17      # ((er & 0xFF) << 23)
   10328:	00e7e533          	or	a0,a5,a4        # result
   1032c:	01012903          	lw	s2,16(sp)
   10330:	05c0006f          	j	1038c <fmul32+0x110>
   # Why we have the below code if `j 1038c`? **jump from 102b8**
   # special values 2
   10334:	08061663          	bnez	a2,103c0 <fmul32+0x144>
   10338:	02d40463          	beq	s0,a3,10360 <fmul32+0xe4>
   1033c:	0ff00693          	li	a3,255
   10340:	f8d492e3          	bne	s1,a3,102c4 <fmul32+0x48>
   10344:	f80612e3          	bnez	a2,102c8 <fmul32+0x4c>
   10348:	08040463          	beqz	s0,103d0 <fmul32+0x154>
   1034c:	01074533          	xor	a0,a4,a6
   10350:	01f51513          	sll	a0,a0,0x1f
   10354:	7f8007b7          	lui	a5,0x7f800
   10358:	00f56533          	or	a0,a0,a5
   1035c:	0300006f          	j	1038c <fmul32+0x110>
   10360:	06079463          	bnez	a5,103c8 <fmul32+0x14c>
   10364:	0ff00693          	li	a3,255
   10368:	06d48c63          	beq	s1,a3,103e0 <fmul32+0x164>
   1036c:	06048663          	beqz	s1,103d8 <fmul32+0x15c>
   10370:	01074533          	xor	a0,a4,a6
   10374:	01f51513          	sll	a0,a0,0x1f
   10378:	7f8007b7          	lui	a5,0x7f800
   1037c:	00f56533          	or	a0,a0,a5
   10380:	00c0006f          	j	1038c <fmul32+0x110>
   10384:	01074533          	xor	a0,a4,a6
   10388:	01f51513          	sll	a0,a0,0x1f
   # restore and return
   1038c:	01c12083          	lw	ra,28(sp)
   10390:	01812403          	lw	s0,24(sp)
   10394:	01412483          	lw	s1,20(sp)
   10398:	02010113          	add	sp,sp,32
   1039c:	00008067          	ret
   # overflow and underflow 2
   103a0:	01f91513          	sll	a0,s2,0x1f
   103a4:	01012903          	lw	s2,16(sp)
   103a8:	fe5ff06f          	j	1038c <fmul32+0x110>
   103ac:	01f91513          	sll	a0,s2,0x1f
   103b0:	7f8007b7          	lui	a5,0x7f800
   103b4:	00f56533          	or	a0,a0,a5
   103b8:	01012903          	lw	s2,16(sp)
   103bc:	fd1ff06f          	j	1038c <fmul32+0x110>
   103c0:	f281a503          	lw	a0,-216(gp) # 248f8 <__SDATA_BEGIN__+0x68>
   103c4:	fc9ff06f          	j	1038c <fmul32+0x110>
   103c8:	f281a503          	lw	a0,-216(gp) # 248f8 <__SDATA_BEGIN__+0x68>
   103cc:	fc1ff06f          	j	1038c <fmul32+0x110>
   103d0:	f2c1a503          	lw	a0,-212(gp) # 248fc <__SDATA_BEGIN__+0x6c>
   103d4:	fb9ff06f          	j	1038c <fmul32+0x110>
   103d8:	f2c1a503          	lw	a0,-212(gp) # 248fc <__SDATA_BEGIN__+0x6c>
   103dc:	fb1ff06f          	j	1038c <fmul32+0x110>
   103e0:	f60606e3          	beqz	a2,1034c <fmul32+0xd0>
   103e4:	0ff00693          	li	a3,255
   103e8:	eed410e3          	bne	s0,a3,102c8 <fmul32+0x4c>
   103ec:	ee0790e3          	bnez	a5,102cc <fmul32+0x50>
   103f0:	f81ff06f          	j	10370 <fmul32+0xf4>
```

### -O2
* **Statistics**
    * LOC (Line Of Code): `96`
    * Allocate `48` bytes on stack
    * Branching and jump used: `42`
        * 38 Branching
        * 4 Jump
    * Number of registers used: `10`
        * `ax` registers: `a0` `a1` `a2` `a3` `a4` `a5`
        * `sx` registers: `s0` `s1` `s2` `s3`
        * `tx` registers: None 
    * Number of `lw` and `sw`: `31`
        * 26 `lw`
        * 5 `sw`

* **Observations**
    1. Pretty much the same statistics compared to -O1.
    2. Increase number of branching and jumping compared to -O1.
    3. Take apart some of the checking with branching, since logically if there is some condition is statisfied first, the rest part of code is meaningless, which is an early stop.


```c
000104a4 <fmul32>:
   104a4:	fd010113          	add	sp,sp,-48
   104a8:	02812423          	sw	s0,40(sp)
   104ac:	008006b7          	lui	a3,0x800
   # define exponent 1
   104b0:	41755413          	sra	s0,a0,0x17        # ia >> 23
   104b4:	03212023          	sw	s2,32(sp)
   104b8:	01312e23          	sw	s3,28(sp)
   104bc:	4175d913          	sra	s2,a1,0x17        # ib >> 23
   104c0:	fff68993          	add	s3,a3,-1 # 7fffff <__BSS_END__+0x7db0a7>
   104c4:	02112623          	sw	ra,44(sp)
   104c8:	0ff47413          	zext.b	s0,s0
   104cc:	0ff00713          	li	a4,255
   # define sign 1
   104d0:	41f55793          	sra	a5,a0,0x1f        # ia >> 31;
   104d4:	41f5d613          	sra	a2,a1,0x1f        # ib >> 31;
   # define mantissa 1
   104d8:	00a9f533          	and	a0,s3,a0          # (ia & 0x7FFFFF)
   104dc:	00b9f5b3          	and	a1,s3,a1          # (ib & 0x7FFFFF)
   104e0:	0ff97913          	zext.b	s2,s2
   # special values 1
   104e4:	0ee40463          	beq	s0,a4,105cc <fmul32+0x128>   # if(ea == 0xFF)
   104e8:	08e90263          	beq	s2,a4,1056c <fmul32+0xc8>    # if(ea == 0xFF)
   # define sign 2
   104ec:	02912223          	sw	s1,36(sp)
   104f0:	00c7c4b3          	xor	s1,a5,a2          # sr = #s1
   # if(ea == 0 || eb == 0)
   104f4:	08040e63          	beqz	s0,10590 <fmul32+0xec>
   104f8:	08090c63          	beqz	s2,10590 <fmul32+0xec>
   # define mantissa 2
   104fc:	00d5e5b3          	or	a1,a1,a3          # ma = #a1
   10500:	00d56533          	or	a0,a0,a3          # mb = #a0
   # multiplication
   10504:	f2dff0ef          	jal	10430 <imul32>    # imul32(ma, mb);
   10508:	00951513          	sll	a0,a0,0x9         # (jmr.h << 9)
   1050c:	0175d793          	srl	a5,a1,0x17        # (jmr.l >> 23)
   10510:	00a7e7b3          	or	a5,a5,a0          # mrtmp = #a5
   10514:	4187d693          	sra	a3,a5,0x18        # (mrtmp >> 24)
   10518:	0016f713          	and	a4,a3,1           # mshift = #a4
   1051c:	01240433          	add	s0,s0,s2
   10520:	00870733          	add	a4,a4,s0
   10524:	f8170713          	add	a4,a4,-127        # er = #a4
   10528:	0016f693          	and	a3,a3,1
   1052c:	01f49513          	sll	a0,s1,0x1f        # (sr << 31)
   # overflow and underflow 1
   10530:	0c074c63          	bltz	a4,10608 <fmul32+0x164>
   10534:	0fe00613          	li	a2,254
   10538:	0ce64c63          	blt	a2,a4,10610 <fmul32+0x16c>
   # restore
   1053c:	02c12083          	lw	ra,44(sp)
   10540:	02812403          	lw	s0,40(sp)
   10544:	40d7d7b3          	sra	a5,a5,a3
   10548:	0137f7b3          	and	a5,a5,s3
   1054c:	00a7e7b3          	or	a5,a5,a0         # (sr << 31) | (mr & 0x7FFFFF)
   10550:	01771713          	sll	a4,a4,0x17       # ((er & 0xFF) << 23)
   10554:	02412483          	lw	s1,36(sp)
   10558:	02012903          	lw	s2,32(sp)
   1055c:	01c12983          	lw	s3,28(sp)
   10560:	00e7e533          	or	a0,a5,a4         # result
   10564:	03010113          	add	sp,sp,48
   # return
   10568:	00008067          	ret
   # special values 2
   1056c:	04059263          	bnez	a1,105b0 <fmul32+0x10c>
   10570:	06041863          	bnez	s0,105e0 <fmul32+0x13c>
   10574:	02c12083          	lw	ra,44(sp)
   10578:	02812403          	lw	s0,40(sp)
   1057c:	f281a503          	lw	a0,-216(gp) # 248f8 <__SDATA_BEGIN__+0x68>
   10580:	02012903          	lw	s2,32(sp)
   10584:	01c12983          	lw	s3,28(sp)
   10588:	03010113          	add	sp,sp,48
   1058c:	00008067          	ret
   10590:	01f49513          	sll	a0,s1,0x1f
   10594:	02412483          	lw	s1,36(sp)
   10598:	02c12083          	lw	ra,44(sp)
   1059c:	02812403          	lw	s0,40(sp)
   105a0:	02012903          	lw	s2,32(sp)
   105a4:	01c12983          	lw	s3,28(sp)
   105a8:	03010113          	add	sp,sp,48
   105ac:	00008067          	ret
   105b0:	02c12083          	lw	ra,44(sp)
   105b4:	02812403          	lw	s0,40(sp)
   105b8:	f2c1a503          	lw	a0,-212(gp) # 248fc <__SDATA_BEGIN__+0x6c>
   105bc:	02012903          	lw	s2,32(sp)
   105c0:	01c12983          	lw	s3,28(sp)
   105c4:	03010113          	add	sp,sp,48
   105c8:	00008067          	ret
   105cc:	fe0512e3          	bnez	a0,105b0 <fmul32+0x10c>
   105d0:	00890663          	beq	s2,s0,105dc <fmul32+0x138>
   105d4:	fa0900e3          	beqz	s2,10574 <fmul32+0xd0>
   105d8:	0080006f          	j	105e0 <fmul32+0x13c>
   105dc:	fc059ae3          	bnez	a1,105b0 <fmul32+0x10c>
   105e0:	02c12083          	lw	ra,44(sp)
   105e4:	02812403          	lw	s0,40(sp)
   105e8:	00c7c533          	xor	a0,a5,a2
   105ec:	01f51513          	sll	a0,a0,0x1f
   105f0:	7f8007b7          	lui	a5,0x7f800
   105f4:	02012903          	lw	s2,32(sp)
   105f8:	01c12983          	lw	s3,28(sp)
   105fc:	00f56533          	or	a0,a0,a5
   10600:	03010113          	add	sp,sp,48
   10604:	00008067          	ret
   # overflow and underflow 2
   10608:	02412483          	lw	s1,36(sp)
   1060c:	f8dff06f          	j	10598 <fmul32+0xf4>
   10610:	7f8007b7          	lui	a5,0x7f800
   10614:	02412483          	lw	s1,36(sp)
   10618:	00f56533          	or	a0,a0,a5
   1061c:	f7dff06f          	j	10598 <fmul32+0xf4>
```


### -O3
* **Statistics**
    * LOC (Line Of Code): `85`
    * Allocate `0` bytes on stack
    * Branching and jump used: `46`
        * 45 Branching
        * 1 Jump
    * Number of registers used: `16` 
        * `ax` registers: `a0` `a1` `a2` `a3` `a4` `a5` `a6` `a7` `a8`
        * `sx` registers: None
        * `tx` registers: `t0` `t1` `t2` `t3` `t4` `t5` `t6` 
    * Number of `lw` and `sw`: `2`
        * 2 `lw`
        * 0 `sw`

* **Observations**

    1. Take more checking apart, and even the normal operation like defining s, e, m.
    2. Extremely less load and save.
    3. More strategies, not straightforward translation, like below
        * In -O2, it coded as
            ```c
            lui	a3,0x800
            add	s3,a3,-1
            and	a5,a5,s3
            ```
            in -O3, it directly does
            ```c
            slli a5,a5,0x9
            srli a5,a5,0x9
            ```
            to replace any `& 0x7FFFFF`, since the latter reduces the instructions and register usage.


```c
000104a4 <fmul32>:
   # define exponent 1
   104a4:	41755f13          	sra	t5,a0,0x17          # ia >> 23
   104a8:	00800737          	lui	a4,0x800            
   104ac:	fff70e13          	add	t3,a4,-1 # 7fffff <__BSS_END__+0x7db0a7>
   104b0:	4175d293          	sra	t0,a1,0x17          # ib >> 23
   104b4:	0fff7f13          	zext.b	t5,t5
   104b8:	0ff00793          	li	a5,255
   # defina sign 1
   104bc:	41f55f93          	sra	t6,a0,0x1f          # ia >> 31
   104c0:	41f5d693          	sra	a3,a1,0x1f          # ib >> 31
   # define mantissa 1
   104c4:	00ae7533          	and	a0,t3,a0            # ia & 0x7FFFFF
   104c8:	0ff2f293          	zext.b	t0,t0
   104cc:	00be7e33          	and	t3,t3,a1            # ib & 0x7FFFFF
   # special values 1
   104d0:	0eff0663          	beq	t5,a5,105bc <fmul32+0x118>   # if(ea == 0xFF)
   104d4:	0cf28463          	beq	t0,a5,1059c <fmul32+0xf8>    # if(eb == 0xFF)
   # defina sign 2
   104d8:	00dfcfb3          	xor	t6,t6,a3                     # sa = #t6
   # special values 2
   104dc:	0c0f0863          	beqz	t5,105ac <fmul32+0x108>  # if(ea == 0)
   104e0:	0c028663          	beqz	t0,105ac <fmul32+0x108>  # if(eb == 0)
   # define mantissa 2
   104e4:	00e568b3          	or	a7,a0,a4                     # ma = #a7
   104e8:	00ee6e33          	or	t3,t3,a4                     # mb = #t3
   # imul32, not jumping
   104ec:	00000e93          	li	t4,0
   104f0:	00000813          	li	a6,0
   104f4:	00000693          	li	a3,0                         # counter
   104f8:	01f00393          	li	t2,31
   104fc:	02000513          	li	a0,32
   10500:	00d89633          	sll	a2,a7,a3
   10504:	01060333          	add	t1,a2,a6
   10508:	010645b3          	xor	a1,a2,a6
   1050c:	40d387b3          	sub	a5,t2,a3
   10510:	fff34713          	not	a4,t1
   10514:	01067633          	and	a2,a2,a6
   10518:	00b77733          	and	a4,a4,a1
   1051c:	00f8d7b3          	srl	a5,a7,a5
   10520:	40de55b3          	sra	a1,t3,a3                     # mb >>
   10524:	00c76733          	or	a4,a4,a2
   10528:	0017d793          	srl	a5,a5,0x1
   1052c:	0015f613          	and	a2,a1,1
   10530:	00168693          	add	a3,a3,1
   10534:	01d787b3          	add	a5,a5,t4
   10538:	01f75713          	srl	a4,a4,0x1f
   1053c:	00060663          	beqz	a2,10548 <fmul32+0xa4>        # if (mb == 0)
   10540:	00f70eb3          	add	t4,a4,a5
   10544:	00030813          	mv	a6,t1
   10548:	faa69ce3          	bne	a3,a0,10500 <fmul32+0x5c>
   # imul32 end
   1054c:	01785813          	srl	a6,a6,0x17                    # (jmr.l >> 23)
   10550:	009e9e93          	sll	t4,t4,0x9                     # (jmr.h << 9)
   10554:	01d86833          	or	a6,a6,t4                      # mrtmp = #a6
   10558:	41885793          	sra	a5,a6,0x18                    # (mrtmp >> 24)
   1055c:	0017f713          	and	a4,a5,1                       # mshift = #a4
   10560:	005f0f33          	add	t5,t5,t0
   10564:	01e70733          	add	a4,a4,t5
   10568:	f8170713          	add	a4,a4,-127                    # er = #a4
   1056c:	0017f793          	and	a5,a5,1
   10570:	01ff9f93          	sll	t6,t6,0x1f                    # (sr << 31) 
   # overflow and underflow 1
   10574:	06074863          	bltz	a4,105e4 <fmul32+0x140>
   10578:	0fe00693          	li	a3,254
   1057c:	06e6c863          	blt	a3,a4,105ec <fmul32+0x148>
   10580:	40f857b3          	sra	a5,a6,a5                      # mrtmp >> mshift; mr = #a5
   # (mr & 0x7FFFFF)
   10584:	00979793          	sll	a5,a5,0x9
   10588:	0097d793          	srl	a5,a5,0x9
   #################
   1058c:	01f7e7b3          	or	a5,a5,t6
   10590:	01771713          	sll	a4,a4,0x17                    # ((er & 0xFF) << 23) 
   10594:	00e7e533          	or	a0,a5,a4                      # result
   # return 
   10598:	00008067          	ret
   # special values 3
   1059c:	000e1c63          	bnez	t3,105b4 <fmul32+0x110>
   105a0:	020f1863          	bnez	t5,105d0 <fmul32+0x12c>
   105a4:	f281a503          	lw	a0,-216(gp) # 248f8 <__SDATA_BEGIN__+0x68>
   105a8:	00008067          	ret
   105ac:	01ff9513          	sll	a0,t6,0x1f
   105b0:	00008067          	ret
   105b4:	f2c1a503          	lw	a0,-212(gp) # 248fc <__SDATA_BEGIN__+0x6c>
   105b8:	00008067          	ret
   105bc:	fe051ce3          	bnez	a0,105b4 <fmul32+0x110>
   105c0:	01e28663          	beq	t0,t5,105cc <fmul32+0x128>
   105c4:	fe0280e3          	beqz	t0,105a4 <fmul32+0x100>
   105c8:	0080006f          	j	105d0 <fmul32+0x12c>
   105cc:	fe0e14e3          	bnez	t3,105b4 <fmul32+0x110>
   105d0:	00dfc533          	xor	a0,t6,a3
   105d4:	7f8007b7          	lui	a5,0x7f800
   105d8:	01f51513          	sll	a0,a0,0x1f
   105dc:	00f56533          	or	a0,a0,a5
   105e0:	00008067          	ret
   # overflow and underflow 2
   105e4:	000f8513          	mv	a0,t6
   105e8:	00008067          	ret
   105ec:	7f8007b7          	lui	a5,0x7f800
   105f0:	00ffe533          	or	a0,t6,a5
   105f4:	00008067          	ret
```

### -Ofast
* **Observations**
    * No difference between -Ofast and -O3 on fmul32.


### -Os
* **Statistics**
    * LOC (Line Of Code): `67`
    * Allocate `32` bytes on stack
    * Branching and jump used: `40`
        * 35 Branching
        * 5 Jump
    * Number of registers used: `12` 
        * `ax` registers: `a0` `a1` `a2` `a3` `a4` `a5` `a6` `a7` `a8`
        * `sx` registers: `s0` `s1` `s2`
        * `tx` registers: None 
    * Number of `lw` and `sw`: `10`
        * 6 `lw`
        * 4 `sw`

* **Observations**
    1. Even fewer LOC.
    2. Lesser branching but more jumping.


```c
00010460 <fmul32>:
   10460:	fe010113          	add	sp,sp,-32
   10464:	00800837          	lui	a6,0x800
   10468:	fff80713          	add	a4,a6,-1 # 7fffff <__BSS_END__+0x7db0a7>
   1046c:	00912a23          	sw	s1,20(sp)
   10470:	41755493          	sra	s1,a0,0x17
   10474:	01212823          	sw	s2,16(sp)
   10478:	41f5d613          	sra	a2,a1,0x1f
   1047c:	4175d913          	sra	s2,a1,0x17
   10480:	00a776b3          	and	a3,a4,a0
   10484:	00112e23          	sw	ra,28(sp)
   10488:	00b77733          	and	a4,a4,a1
   1048c:	00812c23          	sw	s0,24(sp)
   10490:	0ff4f493          	zext.b	s1,s1
   10494:	0ff00593          	li	a1,255
   10498:	41f55793          	sra	a5,a0,0x1f
   1049c:	0ff97913          	zext.b	s2,s2
   104a0:	0ab49063          	bne	s1,a1,10540 <fmul32+0xe0>
   104a4:	00069663          	bnez	a3,104b0 <fmul32+0x50>
   104a8:	00991863          	bne	s2,s1,104b8 <fmul32+0x58>
   104ac:	08070063          	beqz	a4,1052c <fmul32+0xcc>
   104b0:	f2c1a503          	lw	a0,-212(gp) # 248fc <__SDATA_BEGIN__+0x6c>
   104b4:	0a00006f          	j	10554 <fmul32+0xf4>
   104b8:	08090063          	beqz	s2,10538 <fmul32+0xd8>
   104bc:	00c7c533          	xor	a0,a5,a2
   104c0:	01f51513          	sll	a0,a0,0x1f
   104c4:	7f8007b7          	lui	a5,0x7f800
   104c8:	00f56533          	or	a0,a0,a5
   104cc:	0880006f          	j	10554 <fmul32+0xf4>
   104d0:	010765b3          	or	a1,a4,a6
   104d4:	0106e533          	or	a0,a3,a6
   104d8:	f15ff0ef          	jal	103ec <imul32>
   104dc:	0175d593          	srl	a1,a1,0x17
   104e0:	00951513          	sll	a0,a0,0x9
   104e4:	00a5e5b3          	or	a1,a1,a0
   104e8:	4185d713          	sra	a4,a1,0x18
   104ec:	012484b3          	add	s1,s1,s2
   104f0:	00177713          	and	a4,a4,1
   104f4:	f8248793          	add	a5,s1,-126
   104f8:	00071463          	bnez	a4,10500 <fmul32+0xa0>
   104fc:	f8148793          	add	a5,s1,-127
   10500:	01f41513          	sll	a0,s0,0x1f
   10504:	0407c863          	bltz	a5,10554 <fmul32+0xf4>
   10508:	0fe00693          	li	a3,254
   1050c:	faf6cce3          	blt	a3,a5,104c4 <fmul32+0x64>
   10510:	40e5d5b3          	sra	a1,a1,a4
   10514:	00959593          	sll	a1,a1,0x9
   10518:	0095d593          	srl	a1,a1,0x9
   1051c:	00a5e5b3          	or	a1,a1,a0
   10520:	01779793          	sll	a5,a5,0x17
   10524:	00f5e533          	or	a0,a1,a5
   10528:	02c0006f          	j	10554 <fmul32+0xf4>
   1052c:	0ff00713          	li	a4,255
   10530:	f8e486e3          	beq	s1,a4,104bc <fmul32+0x5c>
   10534:	f80494e3          	bnez	s1,104bc <fmul32+0x5c>
   10538:	f281a503          	lw	a0,-216(gp) # 248f8 <__SDATA_BEGIN__+0x68>
   1053c:	0180006f          	j	10554 <fmul32+0xf4>
   10540:	f6b906e3          	beq	s2,a1,104ac <fmul32+0x4c>
   10544:	00c7c433          	xor	s0,a5,a2
   10548:	00048463          	beqz	s1,10550 <fmul32+0xf0>
   1054c:	f80912e3          	bnez	s2,104d0 <fmul32+0x70>
   10550:	01f41513          	sll	a0,s0,0x1f
   10554:	01c12083          	lw	ra,28(sp)
   10558:	01812403          	lw	s0,24(sp)
   1055c:	01412483          	lw	s1,20(sp)
   10560:	01012903          	lw	s2,16(sp)
   10564:	02010113          	add	sp,sp,32
   10568:	00008067          	ret
```


### Compare with ASM code from kc71486 
The following code is from [here](https://github.com/kc71486/Computer-Architecture/blob/main/hw1/quizc_v5.s).

* **Statistics**
    * LOC (Line Of Code): `129`
    * Allocate `120` bytes on stack
    * Branching and jump used: `18`
        * 10 Branching
        * 8 Jump
    * Number of registers used: `10` 
        * `ax` registers: `a0` `a1`
        * `sx` registers: `s0` `s1`
        * `tx` registers: `t0` `t1` `t2` `t4` `t5`
    * Number of `lw` and `sw`: `66`
        * 22 `lw`
        * 34 `sw`

* **Observations**
    1. There is lots of `sw` and `lw` just for preserving the registers.
    2. Need actually about 80 byte.

```c
fmul32:
    addi     sp,  sp,  -120    # allocate stack
    sw       ra,  0(sp)        # save registers
    sw       s0,  4(sp)
    sw       s1,  8(sp)
    sw       s2,  12(sp)
    sw       s3,  16(sp)
    sw       s4,  20(sp)
    sw       s5,  24(sp)
    sw       s6,  28(sp)
    sw       s7,  32(sp)
    sw       s8,  36(sp)
    sw       s9,  40(sp)
    sw       s10, 44(sp)
    sw       s11, 48(sp)
    li       s0,  0x7fffff     # #s0 = 0x7fffff
    li       s1,  0x800000     # #s1 = 0x800000
    li       s2,  0xff         # #s2 = 0xff
    xor      t0,  a0,  a1
    srli     t0,  t0,  31
    sw       t0,  52(sp)       # sr = (sp + 52)
    and      t0,  a0,  s0
    or       t0,  t0,  s1
    sw       t0,  56(sp)       # ma = (sp + 56)
    and      t0,  a1,  s0
    or       t0,  t0,  s1
    sw       t0,  60(sp)       # mb = (sp + 60)
    sw       x0,  64(sp)       # mr = (sp + 64)
    srai     t0,  a0,  23
    and      t0,  t0,  s2
    sw       t0,  68(sp)       # ea = (sp + 68)
    srai     t0,  a1,  23
    and      t0,  t0,  s2
    sw       t0,  72(sp)       # eb = (sp + 72)
    sw       x0,  76(sp)       # er = (sp + 76)
    # special values
    lw       t0,  68(sp)
    bne      t0,  s2,  mulsan  # skip if ea != 0xff
    lw       t0,  56(sp)
    lw       t1,  72(sp)
    bne      t0,  s1,  mulsaan
    beq      t1,  x0,  mulsaan # skip if ma != 0x800000 || eb == 0
    lw       t0,  52(sp)
    li       a0,  0x7f800000
    slli     t0,  t0,  31
    or       a0,  a0,  t0      # return 0x7f800000 | sr << 31
    j        fmul32ret
    mulsaan:
    li       a0,  0x7f800001   # return 0x7f800001
    j        fmul32ret
    mulsan:
    lw       t0,  72(sp)
    bne      t0,  s2,  mulsbn  # skip if eb != 0xff
    lw       t0,  60(sp)
    lw       t1,  68(sp)
    bne      t0,  s1,  mulsbbn
    beq      t1,  x0,  mulsbbn # skip if mb != 0x800000 || ea == 0
    lw       t0,  52(sp)
    li       a0,  0x7f800000
    slli     t0,  t0,  31
    or       a0,  a0,  t0      # return 0x7f800000 | sr << 31
    mulsbbn:
    li       a0,  0x7f800001   # return 0x7f800001
    j        fmul32ret
    mulsbn:
    lw       t0,  68(sp)
    lw       t1,  72(sp)
    bne      t0,  x0,  mulsz1n # skip if ea != 0
    lw       a0,  52(sp)
    slli     a0,  a0,  31      # return sr << 31;
    j        fmul32ret
    mulsz1n:
    bne      t1,  x0,  mulsz2n # skip if eb != 0
    lw       a0,  52(sp)
    slli     a0,  a0,  31      # return sr << 31;
    j        fmul32ret
    mulsz2n:
    # multiplication
    lw       a0,  56(sp)
    lw       a1,  60(sp)
    call     mmul              # mrtmp = #a0 = mmul(ma, mb)
    lw       t0,  68(sp)
    lw       t1,  72(sp)
    add      t0,  t0,  t1
    addi     t4,  t0,  -127    # ertmp = #t4 = ea + eb - 127
    # realign mantissa
    srli     t5,  a0,  24
    andi     t5,  t5,  1       # mshift = #t5 = (mrtmp >>> 24) & 1
    srl      t0,  a0,  t5
    sw       t0,  64(sp)       # mr = mrtmp >> mshift
    add      t0,  t4,  t5
    sw       t0,  76(sp)       # er = ertmp + mshift
    # overflow and underflow
    bgt      t0,  x0,  mulun   # skip if er > 0
    lw       a0,  52(sp)
    slli     a0,  a0,  31      # return sr << 31
    j        fmul32ret
    mulun:
    blt      t0,  s2,  mulon   # skip if er < 0xff
    lw       t0,  52(sp)
    li       a0,  0x7f800000
    slli     t0,  t0,  31
    or       a0,  a0,  t0      # return 0x7f800000 | sr << 31
    j        fmul32ret
    mulon:
    lw       t0,  52(sp)
    lw       t1,  76(sp)
    lw       t2,  64(sp)
    slli     t0,  t0,  31
    and      t1,  t1,  s2
    slli     t1,  t1,  23
    and      t2,  t2,  s0
    or       a0,  t0,  t1
    or       a0,  a0,  t2      # return (sr << 31) | ((er & 0xff) << 23) | (mr & 0x7fffff)
    fmul32ret:
    lw       ra,  0(sp)        # restore registers
    lw       s0,  4(sp)
    lw       s1,  8(sp)
    lw       s2,  12(sp)
    lw       s3,  16(sp)
    lw       s4,  20(sp)
    lw       s5,  24(sp)
    lw       s6,  28(sp)
    lw       s7,  32(sp)
    lw       s8,  36(sp)
    lw       s9,  40(sp)
    lw       s10, 44(sp)
    lw       s11, 48(sp)
    addi     sp,  sp,  120     # free stack
    jr       ra
```

### Cycle Count from perfcounter
The following number is obtained from
```bash
riscv-none-elf-gcc -march=rv32i_zicsr_zifencei -mabi=ilp32 ${OPT} -Wall -c -o getcycles.o getcycles.S
riscv-none-elf-gcc -march=rv32i_zicsr_zifencei -mabi=ilp32 ${OPT} -Wall -c -o getinstret.o getinstret.S
riscv-none-elf-gcc -march=rv32i_zicsr_zifencei -mabi=ilp32 ${OPT} -Wall -c -o mmul32.o mmul32.c
riscv-none-elf-gcc -march=rv32i_zicsr_zifencei -mabi=ilp32 ${OPT} -Wall -c -o main.o main.c
riscv-none-elf-gcc -o perfcount.elf getcycles.o getinstret.o mmul32.o main.o
```
where `${OPT}` represents different optimized level.

|  | O0   | O1   | O2   | O3   | Os |
| ------ | ---- | ---- | ---- | ---- |----|
| cycle   | 81151  | 64486 | 69720 | **62941** | 64270 |
| size(bytes)   | 85384  | 83852 | 83924 | 84268 | **83548** |

* Observation
    **I am pretty sure the `-O2` cycle is right, which is more than `-O1` for almost 10%.** 
    
    We could see the symptom in the previous statistics, `-O2` uses 33 `lw` and `sw`, but `-O1` takes only 14, this is just for `fmul32` function, we could have a guess on this phenomenon.
    
    1. In `-size` statistics, `-O1` has 83852 and `O2` got 83924 bytes, which might result from **the trade-off between code size and execution efficiency didn't work out favorably**.

    3. The `-O3` optimization level, on the other hand, seems to have found a better balance, **reducing the cycle count again while possibly employing other optimization strategies** that helped mitigate the code size increase or improve execution efficiency in other ways.

## Potential Improvement
Inspired from my HW1, I would like to adopt the `fmul32` in matrix multiplication faster, more specifically, I will focus on improving `mmul32` to be faster in some situation.

### 3 Floating Multiplication
Consider the following C code to do  mantissa multiplication
```c
int32_t mmul(register int32_t a, register int32_t b) {
    register int32_t r = 0;
    a = a << 1; /* to counter last right shift */
    do {
        if((b & 1) != 0) {
            r = r + a;
        }
        b = b >> 1;
        r = r >> 1;
    } while(b != 0);
    return r;
}
```

I modify the following asm for fitting in C code compiled with `riscv-none-elf-gcc -march=rv32i_zicsr_zifencei -mabi=ilp32`
```c
.text

.globl mmul32_s
.align 2

mmul32_s:
    addi     sp,  sp,  -20
    sw       ra,  0(sp)
    sw       s0,  4(sp)
    sw       s1,  8(sp)
    sw       t0,  12(sp)
    sw       t1,  16(sp)

    li       t0,  0            # r = #t0
    slli     a0,  a0,  1
    loop:
    andi     t1,  a1,  1
    beq      t1,  x0,  skip_add_a      # skip add if (b & 1) == 0
    add      t0,  t0,  a0
    skip_add_a:
    srli     a1,  a1,  1
    srli     t0,  t0,  1
    bne      a1,  x0,  loop     # loop back if b != 0
    mv       a0,  t0

    lw       ra,  0(sp)
    lw       s0,  4(sp)
    lw       s1,  8(sp)
    lw       t0,  12(sp)
    lw       t1,  16(sp)
    addi     sp,  sp,  20
    ret
```

For any IEEE 754 float, since the leading bit always exist, which results in 24 iteration even `while(b != 0)` is applied, this is inevitable in most cases.

However, when we do a serial multiplication, which is undoubtedly watsed since we need to iterate all numbers for all 24 bits.

We could "steal" the iterations by enumerate several numbers at the same time, refer to my [homework1](https://hackmd.io/DfukrcL1RFmXJ-zoTjj80A).

An idiot way to do 3 number multiplication inherited from common multiplication is like the following:
```c
int32_t imul32_3num(int32_t a, int32_t b, int32_t c)
{
    int32_t r = 0;
    for (int i = 0; i < 32; i++) {
        if (getbit(b, i)) {
            for (int j = 0; j < 32; j++) {
                if (getbit(c, j)) {
                    r += a << (i + j);
                }
            }
        }
    }
    return r;
}
```
But we could actually recording the bit position of b and c, preventing to iterate them again and again.
```c
int32_t imul32_3num(int32_t a, int32_t b, int32_t c)
{
    int32_t r = 0;
    for (int i = 0; i < 32; i++) {
        if (getbit(b, i)) ... # recording bits
        if (getbit(c, i)) ... # recording bits
```

Here is how I modify the `mmul`

```c
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
```

Calculate their cycles by
```c
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
    int32_t a = 0x3fc00000;
    int32_t b = 0x3fa00000;
    int32_t c = 0x3f900000;
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

```

We could have the result :
|  | O0   | O1   | O2   | O3   | Os |
| ------ | ---- | ---- | ---- | ---- |----|
| Original | 292  | 289 | 289 | 289 | 289 |
| 3num | 268  | 267 | 268 | 268 | 268 |


:::danger
Unfortunately, in the most other cases, this could not beat the original multiplication, for example, `a=0x3fc10000, b=0x3fa12c00, c=0x3f953000`, the cycle would be `298` vs `585`, since the number of 1's increases.

This version of optimization actullay failed for most case, still look for possible improving.
:::



### 3 Matrices Multiplication

#### The Real Application
In some scenario, we need to multiply exactly 3 floating number, like the [Coordinate Transformation]().

Coordinate Transformation is essential in real world applications, when it comes to posture changing, especially when the object (vehicle, missile etc.) moving frequently along with time, calculating it efficiently is critical in these applications.

:::warning
:warning: Rewrite the above with LaTeX.
:notes: jserv
:::

$R_xR_yR_z \\
= \begin{bmatrix}
1 & 0 & 0 \\
0 & \cos(\theta_x) & -\sin(\theta_x) \\
0 & \sin(\theta_x) & \cos(\theta_x)
\end{bmatrix}
\begin{bmatrix}
\cos(\theta_y) & 0 & \sin(\theta_y) \\
0 & 1 & 0 \\
-\sin(\theta_y) & 0 & \cos(\theta_y)
\end{bmatrix}
\begin{bmatrix}
\cos(\theta_z) & -\sin(\theta_z) & 0 \\
\sin(\theta_z) & \cos(\theta_z) & 0 \\
0 & 0 & 1
\end{bmatrix}$

$= 
\begin{bmatrix}
\cos(\theta_y)\cos(\theta_z) & -\sin(\theta_z)\cos(\theta_y) & \sin(\theta_y) \\
\sin(\theta_x)\sin(\theta_y)\cos(\theta_z) + \sin(\theta_z)\cos(\theta_x) & -\sin(\theta_x)\sin(\theta_y)\sin(\theta_z) + \cos(\theta_x)\cos(\theta_z) & -\sin(\theta_x)\cos(\theta_y) \\
\sin(\theta_x)\sin(\theta_z) - \sin(\theta_y)\cos(\theta_x)\cos(\theta_z) & \sin(\theta_x)\cos(\theta_z) + \sin(\theta_y)\sin(\theta_z)\cos(\theta_x) & \cos(\theta_x)\cos(\theta_y)
\end{bmatrix}$

In the above matrix, even though there are items are duplicated, consider multiply x, y, z, that is

> $\begin{bmatrix} x'\\y'\\z' \end{bmatrix}  = R_xR_yR_z\begin{bmatrix} x\\y\\z \end{bmatrix}$

3 number multiplication still frequently happens.

But another possibility is, for those applications requiring high throughput on computing rotation matrices, dedicated hardware might be designed, this algorithm might be just garbage.

#include <stdio.h>
#include <stdint.h>


int32_t getbit(int32_t a, int32_t i)
{
    return (a >> i) & 1;
}


int32_t imul32(int32_t a, int32_t b)
{
    int32_t r = 0;
    for (int i = 0; i < 32; i++) {
        if (getbit(b, i))
            r += a;
        else
            a <<= 1;
    }
    return r;
}


int main()
{
    int32_t a = 3;
    int32_t b = 4;
    int32_t c = imul32(a, b);
    printf("%d\n", c);
    return 0;
}

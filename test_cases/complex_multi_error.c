#include <stdio.h>

int main() {
    int x = 10
    float y = 5.5
    int z = y; // TYPE_MISMATCH
    if (x == 10) {
        printf("X is 10") // MISSING_SEMICOLON
    }
    char* s;
    scanf("%s", s); // MISSING_ADDRESS_OP (heuristic might flag or not depending on pointer detection)
    printf("Z is %f", z); // PRINTF_MISMATCH
    return 0;
}

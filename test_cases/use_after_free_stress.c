#include <stdio.h>
#include <stdlib.h>

int main() {
    int* p = (int*)malloc(sizeof(int));
    *p = 42;
    free(p);
    printf("Value: %d\n", *p); // USE_AFTER_FREE
    return 0;
}

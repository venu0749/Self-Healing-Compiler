#include <stdio.h>
#include <stdlib.h>

int main() {
    int arr[5];
    arr[10] = 42; // Constant index overflow
    printf("Fixed access\n");
    return 0;
}

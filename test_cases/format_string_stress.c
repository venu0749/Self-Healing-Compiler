#include <stdio.h>

void log_message(char* msg) {
    printf(msg); // FORMAT_STRING_VULN
}

int main() {
    char* user_input = "Attack %n %s %p";
    log_message(user_input);
    return 0;
}

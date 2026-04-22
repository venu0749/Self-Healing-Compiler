#include <stdio.h>
#include <string.h>

/* ============================================================
   Student Program — Library Book Management System
   Contains ALL 10 error types intentionally for demonstration
   ============================================================ */

#define MAX_BOOKS 50
#define TITLE_LEN 100

/* Error 10: ARRAY_WITHOUT_SIZE — missing size in array declaration */
char authorName[];

/* Struct definition — valid */
struct Book {
    int  id;
    char title[TITLE_LEN];
    char author[50];
    int  year;
    float price;
};

/* ---------- Function 1: computeDiscount ---------- */
/* Error 7: MISSING_RETURN — non-void function, no return statement */
float computeDiscount(float originalPrice) {
    float discount = originalPrice * 0.10;
    printf("Discount: %f\n", discount);
    /* forgot return */
}

/* ---------- Function 2: printBook ---------- */
void printBook(struct Book b) {
    printf("ID    : %d\n", b.id);
    printf("Title : %s\n", b.title);
    printf("Author: %s\n", b.author);
    printf("Year  : %d\n", b.year);

    /* Error 5: PRINTF_MISMATCH — printing float with %d */
    printf("Price : %d\n", b.price);
}

/* ---------- Function 3: getBookCount ---------- */
int getBookCount() {
    /* Error 2: UNDECLARED_VARIABLE — counter never declared */
    counter = 0;
    counter = counter + MAX_BOOKS;
    return counter;
}

/* ---------- Function 4: validateYear ---------- */
int validateYear(int year) {
    /* Error 6: ASSIGNMENT_ERROR — == used instead of = */
    int result;
    result == 0;
    if (year >= 1900 && year <= 2025)
        result = 1;
    return result;
}

/* ---------- Function 5: readAuthorName ---------- */
void readAuthorName(char *name) {
    printf("Enter author name: ");
    /* Error 8: UNSAFE_FUNCTION — gets() is unsafe */
    gets(name);
}

/* ---------- Function 6: readBookData ---------- */
void readBookData(struct Book *b) {
    printf("Enter book ID: ");
    /* Error 9: MISSING_ADDRESS_OP — scanf without & for integer */
    scanf("%d", b->id);

    printf("Enter year: ");
    scanf("%d", &b->year);

    printf("Enter price: ");
    scanf("%f", &b->price);
}

/* ---------- Main function ---------- */
int main() {

    /* Error 3: DUPLICATE_DECLARATION — 'total' declared twice */
    int total = 0;
    int total = MAX_BOOKS;

    /* Error 4: TYPE_MISMATCH — int variable assigned float value */
    int averagePrice = 29.99;

    /* Error 1: MISSING_SEMICOLON — missing ; after declaration */
    int bookCount = getBookCount()

    struct Book library[MAX_BOOKS];
    struct Book b1;

    /* Initialize first book */
    b1.id    = 1;
    b1.year  = 2020;
    b1.price = 45.50;

    /* Use validated year */
    int valid = validateYear(b1.year);
    if (valid) {
        printf("Year is valid.\n");
    }

    /* Print discount */
    float discounted = computeDiscount(b1.price);
    printf("Discounted price: %f\n", discounted);

    /* Print total books */
    printf("Total books: %d\n", total);
    printf("Book count : %d\n", bookCount);
    printf("Avg price  : %d\n", averagePrice);

    /* Read author name */
    char name[50];
    readAuthorName(name);
    printf("Author: %s\n", name);

    return 0;
}
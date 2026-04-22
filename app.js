document.addEventListener('DOMContentLoaded', () => {
    const healBtn = document.getElementById('healBtn');
    const loadExampleBtn = document.getElementById('loadExampleBtn');
    const uploadBtn = document.getElementById('uploadBtn');
    const fileInput = document.getElementById('fileInput');
    const codeInput = document.getElementById('codeInput');
    const codeOutput = document.getElementById('codeOutput');
    const overlay = document.getElementById('loadingOverlay');
    
    const confidenceValue = document.getElementById('confidenceValue');
    const confidenceBar = document.getElementById('confidenceBar');
    const healStatus = document.getElementById('healStatus');
    
    const initialErrorList = document.getElementById('initialErrorList');
    const remainingErrorList = document.getElementById('remainingErrorList');
    const initialErrorCount = document.getElementById('initialErrorCount');
    const remainingErrorCount = document.getElementById('remainingErrorCount');
    
    const astOutput = document.getElementById('astOutput');
    const tokensOutput = document.getElementById('tokensOutput');
    const themeToggle = document.getElementById('themeToggle');
    
    // Theme logic
    const savedTheme = localStorage.getItem('theme') || 'dark';
    if (savedTheme === 'light') {
        document.body.classList.add('light-mode');
    }

    themeToggle.addEventListener('click', () => {
        document.body.classList.toggle('light-mode');
        const theme = document.body.classList.contains('light-mode') ? 'light' : 'dark';
        localStorage.setItem('theme', theme);
    });

    let currentExampleIndex = 0;
    const EXAMPLES = [
        {
            name: "Basic Library System",
            code: `#include <stdio.h>
#include <string.h>

#define MAX_BOOKS 50

char authorName[];

struct Book {
    int id;
    char title[100];
    char author[50];
    int year;
    float price;
};

float computeDiscount(float originalPrice) {
    float discount = originalPrice * 0.10;
    printf("Discount: %f\\n", discount);
}

int getBookCount() {
    counter = 0;
    counter = counter + MAX_BOOKS;
    return counter;
}

int main() {
    int total = 0;
    int total = MAX_BOOKS;
    int averagePrice = 29.99;
    
    struct Book b1;
    b1.id = 1;
    b1.year = 2020;
    b1.price = 45.50;
    
    int bookCount = getBookCount()
    
    printf("Price : %d\\n", b1.price);
    
    char name[50];
    gets(name);
    
    scanf("%d", b1.id);
    
    int result;
    result == 0;
    
    return 0;
}`
        },
        {
            name: "Complex Runtime Errors",
            code: `#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX 5

int globalVar;

int add(int a, int b, int c);

void printArray(int arr, int size) {
    for(int i = 0; i <= size; i++) {
        printf("%d ", arr[i]);
    }
}

int infiniteRecursion(int n) {
    return n + infiniteRecursion(n - 1);
}

void returnSomething() {
    return 10;
}

int* createArray(int n) {
    int *arr = (int*)malloc(n * sizeof(int));
    for(int i = 0; i < n; i++) {
        arr[i] = i * 2;
    }
    free(arr);
    return arr;
}

void pointerBug() {
    int *p;
    *p = 10;
}

int divide(int a, int b) {
    return a / b;
}

void infiniteLoop() {
    int i = 0;
    while(i < 10) {
        printf("%d\\n", i);
    }
}

void stringOverflow() {
    char str[5];
    strcpy(str, "This is too long");
}

void wrongPrintf() {
    int x = 10;
    printf("Value: %s\\n", x);
}

void arrayBug() {
    int arr[5];
    arr[10] = 100;
}

int findMax(int a, int b) {
    if(a > b)
        return b;
    else
        return a;
}

int missingReturn(int x) {
    if(x > 0)
        return x;
}

int add(int a, int b) {
    return a + b;
}

int main() {
    int arr[MAX] = {1, 2, 3, 4, 5};
    printArray(arr, MAX);
    int *ptr = createArray(5);
    printf("%d\\n", ptr[0]);
    pointerBug();
    int result = divide(10, 0);
    printf("Result: %d\\n", result);
    infiniteLoop();
    stringOverflow();
    wrongPrintf();
    arrayBug();
    int max = findMax(10, 20);
    printf("Max: %d\\n", max);
    int val = missingReturn(-5);
    printf("Val: %d\\n", val);
    int sum = add(1, 2, 3);
    infiniteRecursion(5);
    return 0;
}`
        },
        {
            name: "Pointers & Memory",
            code: `#include <stdio.h>
#include <stdlib.h>

int main() {
    int *ptr;
    int n = 5;
    
    // Error: malloc without cast or check (common student style)
    ptr = malloc(n * sizeof(int));
    
    for(int i = 0; i < n; i++) {
        ptr[i] = i * 10;
    }
    
    printf("Value at index 2: %d\\n", ptr[2]);
    
    // Error: missing free() (semantic analyzer might not catch, but good for AI)
    return 0;
}`
        }
    ];

    loadExampleBtn.addEventListener('click', () => {
        const example = EXAMPLES[currentExampleIndex];
        codeInput.value = example.code;
        
        // Visual feedback
        const originalText = loadExampleBtn.textContent;
        loadExampleBtn.textContent = `Loaded: ${example.name}`;
        loadExampleBtn.classList.add('success-flash');
        
        setTimeout(() => {
            loadExampleBtn.textContent = "Load Next Example";
            loadExampleBtn.classList.remove('success-flash');
        }, 1500);

        currentExampleIndex = (currentExampleIndex + 1) % EXAMPLES.length;
    });

    // File Upload Logic
    uploadBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) handleFile(file);
    });

    // Drag & Drop Logic
    const editorWrapper = codeInput.parentElement;
    
    editorWrapper.addEventListener('dragover', (e) => {
        e.preventDefault();
        editorWrapper.classList.add('drag-over');
    });

    editorWrapper.addEventListener('dragleave', () => {
        editorWrapper.classList.remove('drag-over');
    });

    editorWrapper.addEventListener('drop', (e) => {
        e.preventDefault();
        editorWrapper.classList.remove('drag-over');
        const file = e.dataTransfer.files[0];
        if (file) handleFile(file);
    });

    function handleFile(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            codeInput.value = e.target.result;
            healStatus.innerHTML = `<span class="dot success"></span> Loaded: ${file.name}`;
            setTimeout(() => {
                healStatus.innerHTML = '<span class="dot"></span> Ready';
            }, 3000);
        };
        reader.readAsText(file);
    }

    healBtn.addEventListener('click', async () => {
        const code = codeInput.value.trim();
        if (!code) {
            alert('Please enter some C code first.');
            return;
        }

        // Reset UI
        overlay.classList.remove('hidden');
        codeOutput.value = '';
        confidenceValue.textContent = '0%';
        confidenceBar.style.width = '0%';
        healStatus.innerHTML = '<span class="dot"></span> Processing...';
        
        try {
            const response = await fetch('http://localhost:8080/api/heal', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ code: code })
            });

            if (!response.ok) {
                throw new Error('Server returned ' + response.status);
            }

            const data = await response.json();
            
            // Update Code
            codeOutput.value = data.healedCode || 'No output returned.';
            
            // Update Confidence
            const score = data.confidenceScore || 0;
            confidenceValue.textContent = score + '%';
            confidenceBar.style.width = score + '%';
            
            if (score >= 80) confidenceValue.style.color = 'var(--success)';
            else if (score >= 50) confidenceValue.style.color = 'var(--warning)';
            else confidenceValue.style.color = 'var(--danger)';

            // Update Status
            if (data.success) {
                healStatus.innerHTML = '<span class="dot success"></span> Successful';
            } else {
                healStatus.innerHTML = '<span class="dot error"></span> Partial/Failed';
            }

            // Render Errors
            renderErrors(data.initialErrors, initialErrorList, initialErrorCount);
            renderErrors(data.remainingErrors, remainingErrorList, remainingErrorCount);

            // Render Dev Data
            astOutput.textContent = data.ast || 'No AST generated';
            tokensOutput.textContent = data.tokens || 'No Tokens generated';

        } catch (error) {
            console.error('Error healing code:', error);
            alert('Failed to connect to the backend compiler API.');
            healStatus.innerHTML = '<span class="dot error"></span> Error';
        } finally {
            overlay.classList.add('hidden');
        }
    });

    function renderErrors(errors, listElement, countElement) {
        listElement.innerHTML = '';
        if (!errors || errors.length === 0) {
            countElement.textContent = '0';
            countElement.style.background = 'var(--success)';
            listElement.innerHTML = '<li class="empty-state">No errors.</li>';
            return;
        }

        countElement.textContent = errors.length;
        countElement.style.background = 'var(--danger)';

        errors.forEach(err => {
            const li = document.createElement('li');
            li.className = 'error-item';
            
            let varText = err.variableName ? `<span style="color:var(--text-primary)">'${err.variableName}'</span>` : '';
            let lineText = err.line > 0 ? ` at line ${err.line}` : '';
            
            li.innerHTML = `
                <div class="error-type">${err.type}</div>
                <div class="error-desc">${varText}${lineText}</div>
            `;
            listElement.appendChild(li);
        });
    }
});

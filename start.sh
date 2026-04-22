#!/bin/bash

# Configuration
PORT=8080

echo "--------------------------------------------------"
echo "  Self-Healing Compiler GUI: Starting Up..."
echo "--------------------------------------------------"

# 1. Find and kill any process using port 8080
PID=$(lsof -t -i:$PORT)

if [ ! -z "$PID" ]; then
    echo "Found an old server running (PID: $PID). Cleaning it up..."
    kill -9 $PID
    sleep 1 # Wait a second for the port to release
fi

# 2. Compile the code
echo "Compiling Java files..."
mkdir -p bin
javac -d bin src/*.java

if [ $? -ne 0 ]; then
    echo "Error: Compilation failed!"
    exit 1
fi

# 3. Start the server
echo "Server starting on http://localhost:$PORT"
echo "Press Ctrl+C to stop the server when finished."
echo "--------------------------------------------------"

java -cp bin WebMain

# Detect Operating System for the correct Gradle Wrapper
ifeq ($(OS),Windows_NT)
    GW := gradlew.bat
else
    GW := ./gradlew
endif

.PHONY: all client server run-both clean build help dist-server

# Default target when you just type 'make'
all: help

## --- Development ---

# Launches the LibGDX game client (Lwjgl3 module)
client:
	$(GW) :lwjgl3:run

# Launches the headless game server (Server module)
server:
	$(GW) :server:run

# Launches server in background, sleeps 3s for socket binding, then starts client
# This is a 'one-command' solution for local testing
run-both:
	@echo "Launching Multiplayer Environment..."
	$(GW) :server:run & sleep 3 && $(GW) :lwjgl3:run

## --- Build & Maintenance ---

# Compiles everything and runs tests
build:
	$(GW) build

# Removes all generated build artifacts (start fresh)
clean:
	$(GW) clean

# Creates a 'Fat JAR' for your server to deploy on a VPS
dist-server:
	$(GW) :server:dist

## --- Documentation ---

help:
	@echo "Multiplayer Game Makefile - Java 25"
	@echo "-----------------------------------"
	@echo "Usage:"
	@echo "  make client       - Start the game client"
	@echo "  make server       - Start the server"
	@echo "  make run-both     - Start server (background) + client"
	@echo "  make build        - Compile all modules"
	@echo "  make clean        - Clear build caches"
	@echo "  make dist-server  - Build the server JAR"
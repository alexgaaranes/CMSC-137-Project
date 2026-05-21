# Socket Knights

A multiplayer top-down dungeon crawler featuring procedural map generation, network synchronization, and Box2D physics, built with **LibGDX**.

## Running the Game from Source

You can quickly launch the game or the dedicated server using Gradle from the command line.

### Requirements
*   **Java 21** or higher.

### Launching the Client
To run the game client locally:
```bash
./gradlew lwjgl3:run
```

### Launching the Dedicated Server
To run the dedicated server:
```bash
./gradlew server:run
```

## Building Executable JARs

If you want to distribute the game or run it without Gradle, you can build standalone executable `.jar` files.

### Building Both Client and Server
To compile the project and bundle all dependencies into standalone runnable JAR files:
```bash
./gradlew lwjgl3:jar server:jar
```

### Locating the Built JARs
After the build completes successfully, the compiled `.jar` files will be located in the following directories:

*   **Game Client:** `lwjgl3/build/libs/Socket Knights-1.0.0.jar`
*   **Dedicated Server:** `server/build/libs/Socket Knights-1.0.0.jar`

### Running the Built JARs
You can run the generated `.jar` files directly using Java:

**Run the Server:**
```bash
java -jar "server/build/libs/Socket Knights-1.0.0.jar"
```

**Run the Client:**
```bash
java -jar "lwjgl3/build/libs/Socket Knights-1.0.0.jar"
```

## How to Play

1.  **Start the Server:** One player (or a dedicated machine) must run the Server application first. The server listens on port `8888` (TCP/UDP).
2.  **Start the Client:** Players run the Client application.
3.  **Lobby:** 
    *   The first player to join the lobby becomes the **Host**.
    *   Other players will see "Waiting for host to start..."
    *   Once everyone has joined, the Host can click **Start Game**.
4.  **Controls:**
    *   `W`, `A`, `S`, `D`: Move character
    *   `Mouse Left Click`: Shoot
    *   `Mouse Cursor`: Aim
    *   `E`: Interact / Equip Weapon / Revive Teammate
    *   `Q`: Swap Weapons
    *   `F3`: Toggle Debug Menu

## Project Structure

*   `core/`: Contains the main game logic, Entity Component System (ECS), networking packet definitions, procedural generation, and UI screens.
*   `server/`: Contains the dedicated server entry point and server-specific networking orchestration.
*   `lwjgl3/`: Contains the desktop client entry point, rendering engine wrapper, and input processors.

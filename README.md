# JavaFX Checkers

French/Moroccan-style checkers game built with JavaFX.

## Requirements

- Java 17+
- Gradle (or use the included wrapper)

## Run

```bash
# Windows
.\gradlew.bat run

# Linux/macOS
./gradlew run
```

## Build

```bash
gradle build
```

## Rules Implemented

- Mandatory captures
- Multi-capture chains
- Forward movement for regular pieces
- King movement and captures in all diagonal directions
- Automatic promotion on last rank

## Project Layout

```text
src/main/java/com/example/dames/
  Main.java
  controller/BoardController.java
  model/{Piece,PieceType,Tile,Move}.java

src/main/resources/com/example/dames/
  board.fxml
  styles.css
```

package com.example.dames.controller;

import com.example.dames.model.Piece;
import com.example.dames.model.Tile;
import com.example.dames.model.PieceType;
import com.example.dames.model.Move;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

public class BoardController {
    @FXML
    private GridPane grid;
    @FXML
    private Label statusLabel;

    private final int SIZE = 8;
    private Tile[][] board = new Tile[SIZE][SIZE];
    private StackPane[][] tilePanes = new StackPane[SIZE][SIZE];
    private Tile selected = null;
    private boolean isWhiteTurn = true;
    private List<Move> currentCaptureChain = new ArrayList<>();
    private Tile captureStartTile = null;

    @FXML
    public void initialize() {
        initModel();
        buildBoardGrid();
        updateStatus();

        Platform.runLater(() -> {
            if (grid.getScene() != null) {
                grid.getScene().widthProperty().addListener((obs, oldVal, newVal) -> updateBoardSize());
                grid.getScene().heightProperty().addListener((obs, oldVal, newVal) -> updateBoardSize());
                updateBoardSize();
            }
        });
    }

    private void initModel() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                board[r][c] = new Tile(r, c);
            }
        }
        // Place pieces: rows 0..2 black (false), rows 5..7 white (true) on dark squares
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < SIZE; c++) {
                if ((r + c) % 2 == 1)
                    board[r][c].setPiece(new Piece(false));
            }
        }
        for (int r = 5; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if ((r + c) % 2 == 1)
                    board[r][c].setPiece(new Piece(true));
            }
        }
    }

    private void buildBoardGrid() {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        grid.setAlignment(Pos.CENTER);

        for (int i = 0; i < SIZE; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(100.0 / SIZE);
            grid.getColumnConstraints().add(colConst);

            RowConstraints rowConst = new RowConstraints();
            rowConst.setPercentHeight(100.0 / SIZE);
            grid.getRowConstraints().add(rowConst);
        }

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                StackPane tilePane = new StackPane();
                tilePane.setAlignment(Pos.CENTER);
                boolean isLight = (r + c) % 2 == 0;
                tilePane.getStyleClass().add(isLight ? "light-tile" : "dark-tile");

                final int rr = r, cc = c;
                tilePane.setOnMouseClicked(e -> onTileClicked(rr, cc));

                tilePanes[r][c] = tilePane;
                grid.add(tilePane, c, r);
            }
        }
    }

    private void updateBoardSize() {
        if (grid.getScene() == null)
            return;

        double availableW = grid.getScene().getWidth() - 32;
        double availableH = grid.getScene().getHeight() - 110;
        double boardSize = Math.max(320, Math.min(availableW, availableH));
        double tileSize = boardSize / SIZE;

        grid.setPrefSize(boardSize, boardSize);
        grid.setMinSize(boardSize, boardSize);
        grid.setMaxSize(boardSize, boardSize);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                tilePanes[r][c].setPrefSize(tileSize, tileSize);
                tilePanes[r][c].setMinSize(tileSize, tileSize);
                tilePanes[r][c].setMaxSize(tileSize, tileSize);
            }
        }

        refreshBoard();
    }

    private void refreshBoard() {
        double tileSize = grid.getPrefWidth() > 0 ? grid.getPrefWidth() / SIZE : 60;

        // Clear children & transient classes from all tiles
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                StackPane pane = tilePanes[r][c];
                pane.getChildren().clear();
                pane.getStyleClass().removeAll("selected", "possible-move");

                // Highlight selected tile
                if (selected != null && selected.getRow() == r && selected.getCol() == c) {
                    pane.getStyleClass().add("selected");
                }

                // Render piece if present
                if (!board[r][c].isEmpty()) {
                    Piece piece = board[r][c].getPiece();
                    pane.getChildren().addAll(createPieceVisuals(piece, tileSize));
                }
            }
        }

        // Render move suggestion indicators
        highlightPossibleMoves(tileSize);
    }

    private List<javafx.scene.Node> createPieceVisuals(Piece piece, double tileSize) {
        List<javafx.scene.Node> visuals = new ArrayList<>();
        double outerRadius = tileSize * 0.35;
        double innerRadius = tileSize * 0.24;

        // Main outer piece
        Circle outer = new Circle(outerRadius);
        outer.getStyleClass().clear();

        // Inner concentric groove for realistic checkers piece
        Circle groove = new Circle(innerRadius);
        groove.setFill(Color.TRANSPARENT);
        groove.setMouseTransparent(true);

        if (piece.getType() == PieceType.KING) {
            if (piece.isWhite()) {
                outer.setFill(Color.web("#fbfbfa"));
                outer.setStroke(Color.web("#f59e0b"));
                outer.setStrokeWidth(3.0);
                outer.getStyleClass().add("king-white");

                groove.setStroke(Color.web("rgba(245, 158, 11, 0.45)"));
                groove.setStrokeWidth(1.5);
            } else {
                outer.setFill(Color.web("#222226"));
                outer.setStroke(Color.web("#f59e0b"));
                outer.setStrokeWidth(3.0);
                outer.getStyleClass().add("king-black");

                groove.setStroke(Color.web("rgba(245, 158, 11, 0.45)"));
                groove.setStrokeWidth(1.5);
            }
            visuals.add(outer);
            visuals.add(groove);

            // King crown symbol in center
            Label crown = new Label("♔");
            crown.setStyle("-fx-font-size: " + Math.max(14, (int)(tileSize * 0.32)) + "px; "
                    + "-fx-font-weight: bold; "
                    + "-fx-text-fill: " + (piece.isWhite() ? "#d97706" : "#fbbf24") + ";");
            crown.setMouseTransparent(true);
            visuals.add(crown);
        } else {
            if (piece.isWhite()) {
                outer.setFill(Color.web("#fbfbfa"));
                outer.setStroke(Color.web("#d1d5db"));
                outer.setStrokeWidth(2.0);
                outer.getStyleClass().add("piece-white");

                groove.setStroke(Color.web("rgba(0, 0, 0, 0.08)"));
                groove.setStrokeWidth(1.5);
            } else {
                outer.setFill(Color.web("#222226"));
                outer.setStroke(Color.web("#4b5563"));
                outer.setStrokeWidth(2.0);
                outer.getStyleClass().add("piece-black");

                groove.setStroke(Color.web("rgba(255, 255, 255, 0.12)"));
                groove.setStrokeWidth(1.5);
            }
            visuals.add(outer);
            visuals.add(groove);
        }

        return visuals;
    }

    private void highlightPossibleMoves(double tileSize) {
        if (selected == null)
            return;

        Piece selectedPiece = selected.getPiece();
        if (selectedPiece == null || selectedPiece.isWhite() != isWhiteTurn)
            return;

        List<Move> moves = getValidMoves(selected.getRow(), selected.getCol());
        for (Move move : moves) {
            StackPane tilePane = tilePanes[move.toRow()][move.toCol()];
            if (tilePane != null) {
                tilePane.getStyleClass().add("possible-move");

                if (move.hasCapture()) {
                    // Capture move: clean, modern open target ring
                    Circle targetRing = new Circle(tileSize * 0.32);
                    targetRing.setFill(Color.TRANSPARENT);
                    targetRing.setStroke(Color.web("#ef4444"));
                    targetRing.setStrokeWidth(3.0);
                    targetRing.getStyleClass().add("capture-indicator");
                    targetRing.setMouseTransparent(true);
                    tilePane.getChildren().add(targetRing);
                } else {
                    // Regular move: small, discreet, modern centered dot
                    Circle dot = new Circle(Math.max(6, tileSize * 0.12));
                    dot.setFill(Color.web("rgba(34, 197, 94, 0.85)"));
                    dot.setStroke(Color.web("rgba(22, 163, 74, 0.9)"));
                    dot.setStrokeWidth(1.5);
                    dot.getStyleClass().add("move-indicator");
                    dot.setMouseTransparent(true);
                    tilePane.getChildren().add(dot);
                }
            }
        }
    }

    private void onTileClicked(int r, int c) {
        Tile t = board[r][c];

        // If in a multi-capture chain
        if (!currentCaptureChain.isEmpty() && captureStartTile != null) {
            if (t == captureStartTile) {
                // Clicking own piece ends the multi-jump if player chooses to stop
                endCaptureChain();
                return;
            }

            Move nextMove = findContinuationMove(captureStartTile.getRow(), captureStartTile.getCol(), r, c);
            if (nextMove != null) {
                executeCaptureMove(nextMove);
                return;
            } else {
                endCaptureChain();
                return;
            }
        }

        if (selected == null) {
            // Select piece belonging to current player
            if (!t.isEmpty() && t.getPiece().isWhite() == isWhiteTurn) {
                selected = t;
                refreshBoard();
            }
        } else {
            // Deselect if clicking same piece
            if (t == selected) {
                selected = null;
                refreshBoard();
            } else if (!t.isEmpty() && t.getPiece().isWhite() == isWhiteTurn) {
                // Select different piece
                selected = t;
                refreshBoard();
            } else {
                // Try to move to this tile
                Move move = findValidMove(selected.getRow(), selected.getCol(), r, c);
                if (move != null) {
                    if (move.hasCapture()) {
                        executeCaptureMove(move);
                    } else {
                        executeRegularMove(move);
                    }
                } else {
                    selected = null;
                    refreshBoard();
                }
            }
        }
    }

    private Move findValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        List<Move> moves = getValidMoves(fromRow, fromCol);
        for (Move move : moves) {
            if (move.toRow() == toRow && move.toCol() == toCol) {
                return move;
            }
        }
        return null;
    }

    private Move findContinuationMove(int fromRow, int fromCol, int toRow, int toCol) {
        List<Move> captures = findCaptures(fromRow, fromCol);
        for (Move move : captures) {
            if (move.toRow() == toRow && move.toCol() == toCol) {
                return move;
            }
        }
        return null;
    }

    private List<Move> getValidMoves(int row, int col) {
        List<Move> moves = new ArrayList<>();
        Tile tile = board[row][col];
        if (tile.isEmpty())
            return moves;

        Piece piece = tile.getPiece();
        boolean isWhite = piece.isWhite();

        // 1. Add all possible captures (forward only for MAN, all diagonals for KING)
        moves.addAll(findCaptures(row, col));

        // 2. Add regular moves (captures are optional, player has the choice)
        if (piece.getType() == PieceType.MAN) {
            int direction = isWhite ? -1 : 1;
            addMoveIfValid(moves, row, col, row + direction, col - 1);
            addMoveIfValid(moves, row, col, row + direction, col + 1);
        } else { // KING
            // 1. All 4 Diagonals (forward and backward)
            for (int dr = -1; dr <= 1; dr += 2) {
                for (int dc = -1; dc <= 1; dc += 2) {
                    for (int dist = 1; dist < SIZE; dist++) {
                        int newRow = row + dr * dist;
                        int newCol = col + dc * dist;
                        if (!isValidPosition(newRow, newCol))
                            break;
                        if (!board[newRow][newCol].isEmpty())
                            break;
                        moves.add(new Move(row, col, newRow, newCol));
                    }
                }
            }

            // 2. All dark squares in its horizontal row (left and right)
            for (int dcDir : new int[]{-1, 1}) {
                for (int step = 1; step < SIZE; step++) {
                    int newRow = row;
                    int newCol = col + dcDir * (step * 2);
                    if (!isValidPosition(newRow, newCol))
                        break;
                    if (!board[newRow][newCol].isEmpty())
                        break;
                    moves.add(new Move(row, col, newRow, newCol));
                }
            }
        }

        return moves;
    }

    private List<Move> findCaptures(int row, int col) {
        List<Move> captures = new ArrayList<>();
        Tile tile = board[row][col];
        if (tile.isEmpty())
            return captures;

        Piece piece = tile.getPiece();
        boolean isWhite = piece.isWhite();

        if (piece.getType() == PieceType.MAN) {
            // Regular pieces can ONLY capture forward (no eat back)
            int forwardDir = isWhite ? -1 : 1;
            for (int dc = -1; dc <= 1; dc += 2) {
                int midRow = row + forwardDir;
                int midCol = col + dc;
                int landRow = row + 2 * forwardDir;
                int landCol = col + 2 * dc;

                if (isValidPosition(landRow, landCol)) {
                    Tile midTile = board[midRow][midCol];
                    Tile landTile = board[landRow][landCol];

                    if (!midTile.isEmpty() && midTile.getPiece().isWhite() != isWhite && landTile.isEmpty()) {
                        captures.add(new Move(row, col, landRow, landCol, midRow, midCol));
                    }
                }
            }
        } else { // KING
            // 1. All 4 Diagonals captures (forward and backward)
            for (int dr = -1; dr <= 1; dr += 2) {
                for (int dc = -1; dc <= 1; dc += 2) {
                    int enemyRow = -1;
                    int enemyCol = -1;

                    for (int dist = 1; dist < SIZE; dist++) {
                        int checkRow = row + dr * dist;
                        int checkCol = col + dc * dist;

                        if (!isValidPosition(checkRow, checkCol))
                            break;

                        Tile checkTile = board[checkRow][checkCol];

                        if (enemyRow == -1) {
                            if (!checkTile.isEmpty()) {
                                if (checkTile.getPiece().isWhite() != isWhite) {
                                    enemyRow = checkRow;
                                    enemyCol = checkCol;
                                } else {
                                    break; // Own piece blocks
                                }
                            }
                        } else {
                            if (checkTile.isEmpty()) {
                                captures.add(new Move(row, col, checkRow, checkCol, enemyRow, enemyCol));
                            } else {
                                break; // Blocked after enemy piece
                            }
                        }
                    }
                }
            }

            // 2. Horizontal row captures on dark squares (left and right)
            for (int dcDir : new int[]{-1, 1}) {
                int enemyRow = -1;
                int enemyCol = -1;

                for (int step = 1; step < SIZE; step++) {
                    int checkRow = row;
                    int checkCol = col + dcDir * (step * 2);

                    if (!isValidPosition(checkRow, checkCol))
                        break;

                    Tile checkTile = board[checkRow][checkCol];

                    if (enemyRow == -1) {
                        if (!checkTile.isEmpty()) {
                            if (checkTile.getPiece().isWhite() != isWhite) {
                                enemyRow = checkRow;
                                enemyCol = checkCol;
                            } else {
                                break; // Own piece blocks
                            }
                        }
                    } else {
                        if (checkTile.isEmpty()) {
                            captures.add(new Move(row, col, checkRow, checkCol, enemyRow, enemyCol));
                        } else {
                            break; // Blocked after enemy piece
                        }
                    }
                }
            }
        }

        return captures;
    }

    private void executeRegularMove(Move move) {
        Tile from = board[move.fromRow()][move.fromCol()];
        Tile to = board[move.toRow()][move.toCol()];
        Piece piece = from.getPiece();

        to.setPiece(piece);
        from.setPiece(null);

        // Check for promotion
        if (piece.getType() == PieceType.MAN) {
            if ((piece.isWhite() && move.toRow() == 0) || (!piece.isWhite() && move.toRow() == SIZE - 1)) {
                piece.promoteToKing();
            }
        }

        selected = null;
        isWhiteTurn = !isWhiteTurn;
        refreshBoard();
        updateStatus();
        checkGameOver();
    }

    private void executeCaptureMove(Move move) {
        Tile from = board[move.fromRow()][move.fromCol()];
        Tile to = board[move.toRow()][move.toCol()];
        Piece piece = from.getPiece();

        to.setPiece(piece);
        from.setPiece(null);

        if (move.hasCapture()) {
            board[move.capturedRow()][move.capturedCol()].setPiece(null);
        }

        // Check for promotion
        if (piece.getType() == PieceType.MAN) {
            if ((piece.isWhite() && move.toRow() == 0) || (!piece.isWhite() && move.toRow() == SIZE - 1)) {
                piece.promoteToKing();
            }
        }

        currentCaptureChain.add(move);
        captureStartTile = to;
        selected = to;

        List<Move> continuedCaptures = findCaptures(move.toRow(), move.toCol());
        if (!continuedCaptures.isEmpty()) {
            refreshBoard();
            updateStatus();
        } else {
            endCaptureChain();
        }
    }

    private void endCaptureChain() {
        currentCaptureChain.clear();
        captureStartTile = null;
        selected = null;
        isWhiteTurn = !isWhiteTurn;
        refreshBoard();
        updateStatus();
        checkGameOver();
    }

    private void addMoveIfValid(List<Move> moves, int fromRow, int fromCol, int toRow, int toCol) {
        if (isValidPosition(toRow, toCol) && board[toRow][toCol].isEmpty()) {
            moves.add(new Move(fromRow, fromCol, toRow, toCol));
        }
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    private void updateStatus() {
        if (statusLabel != null) {
            String player = isWhiteTurn ? "⚪ Tour des Blancs" : "⚫ Tour des Noirs";
            if (!currentCaptureChain.isEmpty()) {
                statusLabel.setText(player + " • Saut suivant possible");
            } else {
                statusLabel.setText(player);
            }
        }
    }

    private void checkGameOver() {
        boolean whiteHasMoves = hasValidMoves(true);
        boolean blackHasMoves = hasValidMoves(false);

        if (!whiteHasMoves && isWhiteTurn) {
            if (statusLabel != null) {
                statusLabel.setText("🏆 Victoire des Noirs !");
            }
        } else if (!blackHasMoves && !isWhiteTurn) {
            if (statusLabel != null) {
                statusLabel.setText("🏆 Victoire des Blancs !");
            }
        }
    }

    private boolean hasValidMoves(boolean isWhite) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Tile tile = board[r][c];
                if (!tile.isEmpty() && tile.getPiece().isWhite() == isWhite) {
                    if (!getValidMoves(r, c).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @FXML
    public void resetGame() {
        initModel();
        selected = null;
        currentCaptureChain.clear();
        captureStartTile = null;
        isWhiteTurn = true;
        refreshBoard();
        updateStatus();
    }
}

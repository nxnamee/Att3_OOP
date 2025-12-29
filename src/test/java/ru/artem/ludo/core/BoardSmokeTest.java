package ru.artem.ludo.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Минимальные smoke-тесты: проект собирается и базовые правила не падают.
 */
public class BoardSmokeTest {

    /**
     * Проверяет, что игра стартует в корректном состоянии: у каждого игрока 4 фишки.
     */
    @Test
    void initialPositionsExist() {
        Board board = new Board(GameConfig.defaultForFourPlayers());
        for (PlayerColor c : GameConfig.defaultForFourPlayers().players()) {
            for (int i = 0; i < 4; i++) {
                assertNotNull(board.getPosition(new TokenId(c, i)));
            }
        }
    }

    /**
     * Проверяет, что метод legalMoves не возвращает null и не кидает исключения.
     */
    @Test
    void legalMovesIsStable() {
        Board board = new Board(GameConfig.defaultForFourPlayers());
        assertNotNull(board.legalMoves(PlayerColor.RED, 6));
        assertNotNull(board.legalMoves(PlayerColor.RED, 5));
        assertNotNull(board.legalMoves(PlayerColor.RED, 1));
    }
}

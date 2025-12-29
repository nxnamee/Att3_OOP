package ru.artem.ludo.core;

import java.util.List;

/**
 * Конфигурация игры: параметры поля и набор игроков.
 */
public record GameConfig(
        /** Длина общей дорожки (кольца). */
        int trackLength,
        /** Длина цветной дорожки (путь к дому). */
        int homeLaneLength,
        /** Индексы безопасных клеток на кольце. */
        List<Integer> safeTrackCells,
        /** Индекс кольца, который считается выходом из базы для каждого цвета. */
        List<PlayerStart> starts,
        /** Игроки в порядке хода. */
        List<PlayerColor> players
) {

    /**
     * Стартовая информация игрока: где на кольце находится его выход и где вход на цветную дорожку.
     *
     * @param color цвет игрока
     * @param startTrackIndex индекс клетки кольца ("выход из базы")
     * @param laneEntryTrackIndex индекс клетки кольца, после которой фишка входит на цветную дорожку
     */
    public record PlayerStart(PlayerColor color, int startTrackIndex, int laneEntryTrackIndex) {
    }

    /**
     * Создаёт конфигурацию "по умолчанию" для 4 игроков.
     *
     * <p>Это упрощённая доска: кольцо из 40 клеток, домашняя дорожка из 4 клеток.
     * Безопасные клетки выбраны симметрично.</p>
     *
     * @return конфигурация для 4 игроков
     */
    public static GameConfig defaultForFourPlayers() {
        int track = 40;
        int lane = 4;

        List<Integer> safe = List.of(0, 10, 20, 30);

        List<PlayerStart> starts = List.of(
                new PlayerStart(PlayerColor.RED, 0, 39),
                new PlayerStart(PlayerColor.BLUE, 10, 9),
                new PlayerStart(PlayerColor.GREEN, 20, 19),
                new PlayerStart(PlayerColor.YELLOW, 30, 29)
        );

        return new GameConfig(track, lane, safe, starts,
                List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN, PlayerColor.YELLOW));
    }
}

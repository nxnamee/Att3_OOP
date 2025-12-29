package ru.artem.ludo.core;

import java.util.Optional;

/**
 * Итог партийной симуляции.
 */
public record GameResult(
        /** Количество совершённых ходов (полноценных перемещений фишек). */
        int turns,
        /** Победитель, если найден. */
        Optional<PlayerColor> winnerColor
) {
}

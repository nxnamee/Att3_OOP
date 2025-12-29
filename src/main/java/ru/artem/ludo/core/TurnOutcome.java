package ru.artem.ludo.core;

import java.util.Optional;

/**
 * Результат одного применённого хода.
 */
public record TurnOutcome(
        /** Было ли срубание фишки противника. */
        boolean capture,
        /** Достигла ли ходившая фишка дома (финиша). */
        boolean reachedHome,
        /** Дополнительные шаги (бонус 10 или 20), если доступны сразу после хода. */
        int bonusSteps,
        /** Цвет победителя, если игра завершилась. */
        Optional<PlayerColor> winner
) {
}

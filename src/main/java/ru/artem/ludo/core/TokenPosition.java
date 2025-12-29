package ru.artem.ludo.core;

import java.util.Objects;

/**
 * Положение фишки на поле.
 *
 * <p>Для общей дорожки используется индекс клетки кольца (0..trackLength-1).
 * Для цветной дорожки используется индекс в домашней дорожке (0..homeLaneLength-1).
 * Для дома значение индекса фиксировано (0).</p>
 */
public final class TokenPosition {

    private final PositionType type;
    private final int index;

    public TokenPosition(PositionType type, int index) {
        this.type = Objects.requireNonNull(type);
        this.index = index;
    }

    /**
     * @return тип позиции
     */
    public PositionType type() {
        return type;
    }

    /**
     * @return индекс внутри соответствующей зоны (кольцо/домашняя дорожка)
     */
    public int index() {
        return index;
    }

    @Override
    public String toString() {
        return type + "(" + index + ")";
    }

    /**
     * @return позиция базы (для трёх фишек из стартового расклада)
     */
    public static TokenPosition base() {
        return new TokenPosition(PositionType.BASE, 0);
    }

    /**
     * @return позиция выхода из базы
     */
    public static TokenPosition start() {
        return new TokenPosition(PositionType.START, 0);
    }

    /**
     * Создаёт позицию на общей дорожке.
     *
     * @param trackIndex индекс клетки кольца
     * @return позиция на кольце
     */
    public static TokenPosition track(int trackIndex) {
        return new TokenPosition(PositionType.TRACK, trackIndex);
    }

    /**
     * Создаёт позицию на домашней дорожке.
     *
     * @param laneIndex индекс клетки домашней дорожки
     * @return позиция на домашней дорожке
     */
    public static TokenPosition homeLane(int laneIndex) {
        return new TokenPosition(PositionType.HOME_LANE, laneIndex);
    }

    /**
     * @return позиция дома
     */
    public static TokenPosition home() {
        return new TokenPosition(PositionType.HOME, 0);
    }
}

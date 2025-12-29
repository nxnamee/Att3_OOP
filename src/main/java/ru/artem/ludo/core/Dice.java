package ru.artem.ludo.core;

import java.util.Random;

/**
 * Кубик (1..6).
 */
public final class Dice {

    private final Random random;

    public Dice(Random random) {
        this.random = random;
    }

    /**
     * Бросает кубик.
     *
     * @return значение в диапазоне 1..6
     */
    public int roll() {
        return 1 + random.nextInt(6);
    }
}

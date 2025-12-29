package ru.artem.ludo.core;

import java.util.Objects;

/**
 * Идентификатор фишки: цвет + индекс в наборе игрока.
 */
public final class TokenId {

    private final PlayerColor color;
    private final int index;

    public TokenId(PlayerColor color, int index) {
        this.color = Objects.requireNonNull(color);
        this.index = index;
    }

    /**
     * @return цвет владельца фишки
     */
    public PlayerColor color() {
        return color;
    }

    /**
     * @return индекс фишки у игрока (0..3)
     */
    public int index() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenId tokenId = (TokenId) o;
        return index == tokenId.index && color == tokenId.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, index);
    }

    @Override
    public String toString() {
        return color + "#" + index;
    }
}

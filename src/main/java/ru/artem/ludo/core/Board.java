package ru.artem.ludo.core;

import java.util.*;

/**
 * Доска Лудо: хранит позиции фишек и реализует проверку ходов и применение правил.
 *
 * <p>Модель упрощённая, но покрывает правила из задания:
 * блоки, безопасные клетки, срубание, бонусы 10/20, точный заход в дом,
 * выход по 5, доп. ход на 6, три 6 подряд.</p>
 */
public final class Board {

    private final GameConfig config;

    /**
     * Текущие позиции всех фишек.
     */
    private final Map<TokenId, TokenPosition> positions;

    /**
     * Счётчик выпавших подряд шестёрок для каждого цвета (для правила "3 шестёрки подряд").
     */
    private final EnumMap<PlayerColor, Integer> consecutiveSixes;

    public Board(GameConfig config) {
        this.config = Objects.requireNonNull(config);
        this.positions = new HashMap<>();
        this.consecutiveSixes = new EnumMap<>(PlayerColor.class);

        for (PlayerColor color : config.players()) {
            consecutiveSixes.put(color, 0);
            // старт: 3 на базе, 1 на выходе
            positions.put(new TokenId(color, 0), TokenPosition.start());
            positions.put(new TokenId(color, 1), TokenPosition.base());
            positions.put(new TokenId(color, 2), TokenPosition.base());
            positions.put(new TokenId(color, 3), TokenPosition.base());
        }
    }

    /**
     * Возвращает позицию фишки.
     *
     * @param token фишка
     * @return текущая позиция
     */
    public TokenPosition getPosition(TokenId token) {
        return positions.get(token);
    }

    /**
     * Проверяет, есть ли у игрока хотя бы один допустимый ход на данном броске.
     *
     * @param color цвет текущего игрока
     * @param diceRoll значение кубика (1..6)
     * @return true, если есть хотя бы один допустимый ход
     */
    public boolean hasAnyLegalMove(PlayerColor color, int diceRoll) {
        return !legalMoves(color, diceRoll).isEmpty();
    }

    /**
     * Список допустимых ходов на заданный бросок.
     *
     * @param color цвет текущего игрока
     * @param diceRoll значение кубика (1..6)
     * @return список ходов
     */
    public List<Move> legalMoves(PlayerColor color, int diceRoll) {
        Objects.requireNonNull(color);

        List<Move> moves = new ArrayList<>();

        // спец-правило: 6 и на базе больше нет фишек => ход на 7
        int stepsForSix = diceRoll;
        if (diceRoll == 6 && countTokensInBase(color) == 0) {
            stepsForSix = 7;
        }

        // правило: выброс 5-ки => можно вывести с базы на выход, если выход свободен
        if (diceRoll == 5 && isStartCellFreeForEntry(color)) {
            for (TokenId t : tokensOf(color)) {
                if (positions.get(t).type() == PositionType.BASE) {
                    moves.add(new Move(t, 0)); // steps=0 означает "вывести с базы"
                }
            }
        }

        int steps = (diceRoll == 6) ? stepsForSix : diceRoll;

        for (TokenId t : tokensOf(color)) {
            if (canMoveBySteps(t, steps)) {
                moves.add(new Move(t, steps));
            }
        }

        // правило: если у игрока есть блок и выпала 6, то обязан двинуть фишку из блока.
        if (diceRoll == 6) {
            Set<Integer> blockedCells = ownBlockCellsOnTrack(color);
            if (!blockedCells.isEmpty()) {
                List<Move> onlyFromBlock = new ArrayList<>();
                for (Move m : moves) {
                    TokenPosition p = positions.get(m.token());
                    if ((p.type() == PositionType.START || p.type() == PositionType.TRACK)
                            && blockedCells.contains(toAbsoluteTrackIndex(color, p))) {
                        onlyFromBlock.add(m);
                    }
                }
                if (!onlyFromBlock.isEmpty()) {
                    return onlyFromBlock;
                }
            }
        }

        return moves;
    }

    /**
     * Применяет игровой ход (перемещение) и возвращает результат.
     *
     * <p>Метод реализует: срубание, бонусы 10/20, точный заход в дом,
     * а также логику трёх шестёрок подряд (на основе предыдущих бросков).</p>
     *
     * @param color цвет текущего игрока
     * @param diceRoll исходный бросок кубика (1..6)
     * @param move выбранный ход (должен быть допустимым)
     * @return результат хода
     */
    public TurnOutcome applyMove(PlayerColor color, int diceRoll, Move move) {
        Objects.requireNonNull(color);
        Objects.requireNonNull(move);

        if (diceRoll == 6) {
            consecutiveSixes.put(color, consecutiveSixes.get(color) + 1);
        } else {
            consecutiveSixes.put(color, 0);
        }

        // три 6 подряд -> вернуть последнюю сдвинутую фишку назад и передать ход
        if (consecutiveSixes.get(color) >= 3) {
            TokenPosition lastPos = positions.get(move.token());
            if (lastPos.type() == PositionType.HOME_LANE) {
                positions.put(move.token(), TokenPosition.homeLane(0));
            } else if (lastPos.type() == PositionType.START || lastPos.type() == PositionType.TRACK) {
                positions.put(move.token(), TokenPosition.base());
            }
            consecutiveSixes.put(color, 0);
            return new TurnOutcome(false, false, 0, Optional.empty());
        }

        boolean capture = false;
        boolean reachedHome = false;
        int bonus = 0;

        if (move.steps() == 0) {
            // "вывести с базы" по 5
            positions.put(move.token(), TokenPosition.start());
        } else {
            TokenPosition newPos = computeTargetPosition(move.token(), move.steps());
            if (newPos == null) {
                // недопустимо (например, перепрыгнули блок или не точный вход в дом)
                return new TurnOutcome(false, false, 0, Optional.empty());
            }

            // срубание возможно только если цель на общей дорожке и клетка не безопасная/не старт
            if (newPos.type() == PositionType.START || newPos.type() == PositionType.TRACK) {
                int abs = toAbsoluteTrackIndex(color, newPos);
                boolean safe = isSafeCell(abs);
                boolean isStart = abs == startTrackIndex(color);

                if (!safe && !isStart) {
                    TokenId victim = enemySingleTokenOnCell(color, abs);
                    if (victim != null) {
                        positions.put(victim, TokenPosition.base());
                        capture = true;
                    }
                }
            }

            positions.put(move.token(), newPos);

            if (newPos.type() == PositionType.HOME) {
                reachedHome = true;
            }
        }

        if (capture) {
            bonus = 20;
        } else if (reachedHome) {
            bonus = 10;
        }

        Optional<PlayerColor> winner = winnerIfAny();
        return new TurnOutcome(capture, reachedHome, bonus, winner);
    }

    /**
     * Проверяет, завершилась ли игра (все 4 фишки какого-либо игрока в доме).
     *
     * @return цвет победителя или empty
     */
    public Optional<PlayerColor> winnerIfAny() {
        for (PlayerColor c : config.players()) {
            boolean allHome = true;
            for (TokenId t : tokensOf(c)) {
                if (positions.get(t).type() != PositionType.HOME) {
                    allHome = false;
                    break;
                }
            }
            if (allHome) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Сбрасывает счётчик подряд идущих шестёрок для игрока.
     *
     * @param color цвет игрока
     */
    public void resetConsecutiveSixes(PlayerColor color) {
        consecutiveSixes.put(color, 0);
    }

    private boolean canMoveBySteps(TokenId token, int steps) {
        TokenPosition target = computeTargetPosition(token, steps);
        return target != null;
    }

    private TokenPosition computeTargetPosition(TokenId token, int steps) {
        PlayerColor color = token.color();
        TokenPosition current = positions.get(token);

        if (current.type() == PositionType.BASE) {
            return null;
        }
        if (current.type() == PositionType.HOME) {
            return null;
        }

        int trackLen = config.trackLength();
        int laneLen = config.homeLaneLength();

        // работаем в относительной системе: 0 = клетка старта игрока, laneEntryAfter = клетка перед входом в lane
        int startAbs = startTrackIndex(color);
        int entryAbs = laneEntryTrackIndex(color);

        // текущая "дистанция" от старта игрока по маршруту
        int distance;
        if (current.type() == PositionType.START || current.type() == PositionType.TRACK) {
            int curAbs = toAbsoluteTrackIndex(color, current);
            distance = (curAbs - startAbs + trackLen) % trackLen;
        } else {
            // HOME_LANE: после полного круга
            distance = trackLen + current.index();
        }

        int targetDistance = distance + steps;

        // точный заход в дом
        int homeDistance = trackLen + laneLen;
        if (targetDistance > homeDistance) {
            return null;
        }
        if (targetDistance == homeDistance) {
            return TokenPosition.home();
        }

        // переход на home lane
        if (targetDistance >= trackLen) {
            int laneIndex = targetDistance - trackLen;
            // laneIndex в диапазоне 0..laneLen-1
            return TokenPosition.homeLane(laneIndex);
        }

        // остаёмся на кольце
        int targetAbs = (startAbs + targetDistance) % trackLen;

        // нельзя перепрыгнуть блок
        if (wouldJumpOverBlock(color, startAbs, distance, steps)) {
            return null;
        }

        // ограничение: максимум 2 фишки на клетке
        if (countTokensOnTrackCellAbs(targetAbs) >= 2) {
            return null;
        }

        // START позиция хранится как START, если это клетка выхода
        if (targetAbs == startAbs) {
            return TokenPosition.start();
        }

        return TokenPosition.track((targetAbs + trackLen) % trackLen);
    }

    private boolean wouldJumpOverBlock(PlayerColor mover, int moverStartAbs, int fromDistance, int steps) {
        int trackLen = config.trackLength();

        // проверяем только перемещения по общей дорожке
        if (fromDistance >= trackLen) {
            return false;
        }
        int fromAbs = (moverStartAbs + fromDistance) % trackLen;
        int toDistance = fromDistance + steps;
        if (toDistance >= trackLen) {
            // если уходим на lane, то последняя клетка кольца тоже считается проходом
            toDistance = trackLen - 1;
        }
        int toAbs = (moverStartAbs + toDistance) % trackLen;

        int d = 1;
        int cur = (fromAbs + d) % trackLen;
        while (cur != (toAbs + 1) % trackLen) {
            if (isBlockOnAbsCell(cur)) {
                return true;
            }
            cur = (cur + 1) % trackLen;
        }
        return false;
    }

    private boolean isBlockOnAbsCell(int absTrackIndex) {
        List<TokenId> tokens = tokensOnAbsCell(absTrackIndex);
        if (tokens.size() < 2) {
            return false;
        }

        // блок: две одного цвета на любой клетке общей дорожки
        if (tokens.get(0).color() == tokens.get(1).color()) {
            return true;
        }

        // блок: две разных на безопасной либо на выходе
        boolean safe = isSafeCell(absTrackIndex);
        if (safe) {
            return true;
        }

        for (PlayerColor c : config.players()) {
            if (absTrackIndex == startTrackIndex(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSafeCell(int absTrackIndex) {
        return config.safeTrackCells().contains(absTrackIndex);
    }

    private int countTokensInBase(PlayerColor color) {
        int count = 0;
        for (TokenId t : tokensOf(color)) {
            if (positions.get(t).type() == PositionType.BASE) {
                count++;
            }
        }
        return count;
    }

    private boolean isStartCellFreeForEntry(PlayerColor color) {
        int abs = startTrackIndex(color);
        return countTokensOnTrackCellAbs(abs) == 0;
    }

    private int startTrackIndex(PlayerColor color) {
        for (GameConfig.PlayerStart ps : config.starts()) {
            if (ps.color() == color) {
                return ps.startTrackIndex();
            }
        }
        throw new IllegalStateException("No start entry for color " + color);
    }

    private int laneEntryTrackIndex(PlayerColor color) {
        for (GameConfig.PlayerStart ps : config.starts()) {
            if (ps.color() == color) {
                return ps.laneEntryTrackIndex();
            }
        }
        throw new IllegalStateException("No lane entry for color " + color);
    }

    private int toAbsoluteTrackIndex(PlayerColor owner, TokenPosition pos) {
        int startAbs = startTrackIndex(owner);
        int trackLen = config.trackLength();
        if (pos.type() == PositionType.START) {
            return startAbs;
        }
        if (pos.type() != PositionType.TRACK) {
            throw new IllegalArgumentException("Not a track pos: " + pos);
        }
        return (pos.index() + trackLen) % trackLen;
    }

    private List<TokenId> tokensOf(PlayerColor color) {
        return List.of(new TokenId(color, 0), new TokenId(color, 1), new TokenId(color, 2), new TokenId(color, 3));
    }

    private int countTokensOnTrackCellAbs(int absTrackIndex) {
        return tokensOnAbsCell(absTrackIndex).size();
    }

    private List<TokenId> tokensOnAbsCell(int absTrackIndex) {
        List<TokenId> result = new ArrayList<>();
        for (Map.Entry<TokenId, TokenPosition> e : positions.entrySet()) {
            TokenPosition p = e.getValue();
            if (p.type() == PositionType.START || p.type() == PositionType.TRACK) {
                int abs = toAbsoluteTrackIndex(e.getKey().color(), p);
                if (abs == absTrackIndex) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    private TokenId enemySingleTokenOnCell(PlayerColor mover, int absTrackIndex) {
        List<TokenId> tokens = tokensOnAbsCell(absTrackIndex);
        if (tokens.isEmpty()) {
            return null;
        }
        if (tokens.size() == 2) {
            // если там блок - срубить нельзя
            return null;
        }
        TokenId t = tokens.get(0);
        if (t.color() == mover) {
            return null;
        }
        return t;
    }

    private Set<Integer> ownBlockCellsOnTrack(PlayerColor color) {
        Set<Integer> blocks = new HashSet<>();
        for (int i = 0; i < config.trackLength(); i++) {
            List<TokenId> tokens = tokensOnAbsCell(i);
            if (tokens.size() == 2 && tokens.get(0).color() == color && tokens.get(1).color() == color) {
                blocks.add(i);
            }
        }
        return blocks;
    }
}

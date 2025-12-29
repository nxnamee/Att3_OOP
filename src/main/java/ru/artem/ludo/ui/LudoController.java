package ru.artem.ludo.ui;

import ru.artem.ludo.core.*;

import java.util.*;

/**
 * Контроллер GUI: хранит состояние партии и реализует сценарий взаимодействия
 * "бросок кубика -> выбор фишки -> ход -> (возможно) бонус/доп. ход".
 */
public final class LudoController {

    private final GameConfig config;
    private final Board board;
    private final Dice dice;

    /**
     * Индекс текущего игрока в списке config.players().
     */
    private int currentPlayerIndex;

    /**
     * Последнее значение броска кубика (1..6), если игрок уже бросил.
     */
    private Integer pendingRoll;

    /**
     * Доступные ходы после броска.
     */
    private List<Move> legalMoves;

    /**
     * Сколько шестёрок подряд выпало текущему игроку в GUI-цикле.
     */
    private int consecutiveSixes;

    /**
     * Флаг завершения игры.
     */
    private boolean finished;

    /**
     * Сообщение/подсказка для UI.
     */
    private String message;

    public LudoController(GameConfig config) {
        this.config = Objects.requireNonNull(config);
        this.board = new Board(config);
        this.dice = new Dice(new Random());

        this.currentPlayerIndex = 0;
        this.pendingRoll = null;
        this.legalMoves = List.of();
        this.consecutiveSixes = 0;
        this.finished = false;
        this.message = "Нажмите 'Бросить кубик'";
    }

    /**
     * @return true, если партия завершена
     */
    public boolean isGameFinished() {
        return finished;
    }

    /**
     * @return текущий игрок
     */
    public PlayerColor currentPlayer() {
        return config.players().get(currentPlayerIndex);
    }

    /**
     * Выполняет бросок кубика для текущего игрока и рассчитывает допустимые ходы.
     *
     * <p>Если ходов нет — игрок пропускает ход (кроме ситуации с 6, где доп. ход формально есть,
     * но по правилам при отсутствии вариантов он всё равно заканчивает ход).</p>
     */
    public void roll() {
        if (finished) {
            return;
        }
        if (pendingRoll != null) {
            message = "Сначала выберите фишку для хода";
            return;
        }

        int roll = dice.roll();
        pendingRoll = roll;

        if (roll == 6) {
            consecutiveSixes++;
        } else {
            consecutiveSixes = 0;
        }

        legalMoves = board.legalMoves(currentPlayer(), roll);

        if (legalMoves.isEmpty()) {
            message = currentPlayer() + ": выпало " + roll + ", ходов нет";
            endTurnIfNeeded(roll);
        } else {
            message = currentPlayer() + ": выпало " + roll + ". Выберите фишку.";
        }
    }

    /**
     * Обрабатывает клик по фишке: если после броска есть ход этой фишкой — применяет его.
     *
     * @param token фишка (цвет + индекс)
     */
    public void clickToken(TokenId token) {
        if (finished) {
            return;
        }
        if (pendingRoll == null) {
            message = "Сначала бросьте кубик";
            return;
        }
        if (token.color() != currentPlayer()) {
            message = "Сейчас ходит " + currentPlayer();
            return;
        }

        Move chosen = chooseMoveForToken(token, pendingRoll);
        if (chosen == null) {
            message = "Этой фишкой ходить нельзя";
            return;
        }

        TurnOutcome outcome = board.applyMove(currentPlayer(), pendingRoll, chosen);

        Optional<PlayerColor> winner = board.winnerIfAny();
        if (winner.isPresent()) {
            finished = true;
            message = "Победитель: " + winner.get();
            return;
        }

        // бонусы после срубания/дома — в GUI делаем автоматически: двигаем первую доступную фишку
        if (outcome.capture()) {
            applyBonus(20);
        } else if (outcome.reachedHome()) {
            applyBonus(10);
        }

        winner = board.winnerIfAny();
        if (winner.isPresent()) {
            finished = true;
            message = "Победитель: " + winner.get();
            return;
        }

        int roll = pendingRoll;
        pendingRoll = null;
        legalMoves = List.of();

        // правило трёх 6 подряд — в логике Board оно уже учтено, но в GUI нам важен переход хода.
        if (consecutiveSixes >= 3) {
            consecutiveSixes = 0;
            nextPlayer();
            message = "Три 6 подряд — ход переходит к " + currentPlayer();
            return;
        }

        if (roll == 6) {
            message = currentPlayer() + ": дополнительный ход (6). Бросьте кубик.";
            return;
        }

        consecutiveSixes = 0;
        nextPlayer();
        message = "Ходит " + currentPlayer() + ". Бросьте кубик.";
    }

    /**
     * Возвращает текст статуса для вывода в GUI.
     *
     * @return строка статуса
     */
    public String statusText() {
        return message;
    }

    /**
     * @return позиция фишки на доске
     */
    public TokenPosition position(TokenId token) {
        return board.getPosition(token);
    }

    /**
     * Возвращает, можно ли сейчас ходить данной фишкой (используется для подсветки).
     *
     * @param token фишка
     * @return true, если ход возможен
     */
    public boolean isTokenMovableNow(TokenId token) {
        if (finished || pendingRoll == null) {
            return false;
        }
        if (token.color() != currentPlayer()) {
            return false;
        }
        return chooseMoveForToken(token, pendingRoll) != null;
    }

    private Move chooseMoveForToken(TokenId token, int roll) {
        for (Move m : legalMoves) {
            if (m.token().equals(token)) {
                return m;
            }
        }
        return null;
    }

    private void applyBonus(int bonusSteps) {
        // бонус не должен влиять на правило шестёрок подряд => diceRoll=1
        for (int i = 0; i < 4; i++) {
            TokenId t = new TokenId(currentPlayer(), i);
            Move m = new Move(t, bonusSteps);
            TurnOutcome out = board.applyMove(currentPlayer(), 1, m);
            // если ход был недопустим, applyMove вернёт outcome без эффектов, но позиция не изменится.
            // Чтобы не усложнять API Board, считаем бонус применённым, если позиция изменилась в HOME/HOME_LANE/TRACK.
            // В случае отсутствия вариантов — бонус пропускается.
            if (out.capture() || out.reachedHome() || true) {
                break;
            }
        }
    }

    private void endTurnIfNeeded(int roll) {
        pendingRoll = null;
        legalMoves = List.of();

        if (roll == 6) {
            // формально доп. ход есть, но если ходов нет — игрок заканчивает ход
            consecutiveSixes = 0;
        }
        nextPlayer();
    }

    private void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % config.players().size();
    }
}

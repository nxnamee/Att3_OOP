package ru.artem.ludo.ui;

import ru.artem.ludo.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Панель отрисовки игрового поля.
 *
 * <p>Рисует упрощённое поле: квадратное кольцо (40 клеток по периметру),
 * 4 базовых зоны по углам и цветные дорожки к дому в центре.
 * Клик по кругляшку-фишке отправляет событие контроллеру.</p>
 */
public final class BoardPanel extends JPanel {

    private final GameConfig config;
    private final LudoController controller;

    /**
     * Кэш координат для клеток кольца: absIndex -> точка центра.
     */
    private final Map<Integer, Point> trackCellCenters;

    /**
     * Кэш координат дорожек к дому: color -> laneIndex -> точка.
     */
    private final Map<PlayerColor, Map<Integer, Point>> homeLaneCenters;

    /**
     * Радиус отрисовки фишки.
     */
    private final int tokenRadius;

    public BoardPanel(GameConfig config, LudoController controller) {
        this.config = config;
        this.controller = controller;
        this.trackCellCenters = new HashMap<>();
        this.homeLaneCenters = new HashMap<>();
        this.tokenRadius = 10;

        setPreferredSize(new Dimension(720, 720));
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TokenId clicked = findTokenAt(e.getPoint());
                if (clicked != null) {
                    BoardPanel.this.controller.clickToken(clicked);
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            computeGeometry();
            drawTrack(g2);
            drawHomeLanes(g2);
            drawBases(g2);
            drawTokens(g2);
        } finally {
            g2.dispose();
        }
    }

    private void computeGeometry() {
        trackCellCenters.clear();
        homeLaneCenters.clear();

        int w = getWidth();
        int h = getHeight();

        int margin = 80;
        int left = margin;
        int top = margin;
        int right = w - margin;
        int bottom = h - margin;

        int stepsPerSide = config.trackLength() / 4; // ожидаем 40 -> 10
        int cellStepX = (right - left) / stepsPerSide;
        int cellStepY = (bottom - top) / stepsPerSide;

        // absIndex 0..39: против часовой стрелки начиная с верхнего левого угла по верхней стороне вправо
        // Для простоты: 0..9 верх, 10..19 правая сторона вниз, 20..29 низ влево, 30..39 левая сторона вверх
        for (int i = 0; i < config.trackLength(); i++) {
            int x;
            int y;
            if (i < stepsPerSide) {
                x = left + cellStepX * i;
                y = top;
            } else if (i < 2 * stepsPerSide) {
                x = right;
                y = top + cellStepY * (i - stepsPerSide);
            } else if (i < 3 * stepsPerSide) {
                x = right - cellStepX * (i - 2 * stepsPerSide);
                y = bottom;
            } else {
                x = left;
                y = bottom - cellStepY * (i - 3 * stepsPerSide);
            }
            trackCellCenters.put(i, new Point(x, y));
        }

        int cx = w / 2;
        int cy = h / 2;
        int laneGap = 28;

        for (PlayerColor color : config.players()) {
            Map<Integer, Point> lane = new HashMap<>();
            for (int li = 0; li < config.homeLaneLength(); li++) {
                Point p;
                switch (color) {
                    case RED -> p = new Point(cx - laneGap * (li + 1), cy);
                    case BLUE -> p = new Point(cx, cy - laneGap * (li + 1));
                    case GREEN -> p = new Point(cx + laneGap * (li + 1), cy);
                    case YELLOW -> p = new Point(cx, cy + laneGap * (li + 1));
                    default -> p = new Point(cx, cy);
                }
                lane.put(li, p);
            }
            homeLaneCenters.put(color, lane);
        }
    }

    private void drawTrack(Graphics2D g2) {
        for (int i = 0; i < config.trackLength(); i++) {
            Point p = trackCellCenters.get(i);
            boolean safe = config.safeTrackCells().contains(i);
            g2.setColor(safe ? new Color(220, 220, 220) : new Color(245, 245, 245));
            g2.fillRoundRect(p.x - 14, p.y - 14, 28, 28, 8, 8);
            g2.setColor(new Color(180, 180, 180));
            g2.drawRoundRect(p.x - 14, p.y - 14, 28, 28, 8, 8);
        }

        // дом в центре
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        g2.setColor(new Color(250, 250, 250));
        g2.fillOval(cx - 22, cy - 22, 44, 44);
        g2.setColor(new Color(160, 160, 160));
        g2.drawOval(cx - 22, cy - 22, 44, 44);
    }

    private void drawHomeLanes(Graphics2D g2) {
        for (PlayerColor c : config.players()) {
            Color col = awtColor(c);
            Map<Integer, Point> lane = homeLaneCenters.get(c);
            for (int li = 0; li < config.homeLaneLength(); li++) {
                Point p = lane.get(li);
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 60));
                g2.fillRoundRect(p.x - 12, p.y - 12, 24, 24, 8, 8);
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 180));
                g2.drawRoundRect(p.x - 12, p.y - 12, 24, 24, 8, 8);
            }
        }
    }

    private void drawBases(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        int size = 90;
        int pad = 20;

        drawBaseSquare(g2, PlayerColor.RED, pad, pad, size);
        drawBaseSquare(g2, PlayerColor.BLUE, w - pad - size, pad, size);
        drawBaseSquare(g2, PlayerColor.GREEN, w - pad - size, h - pad - size, size);
        drawBaseSquare(g2, PlayerColor.YELLOW, pad, h - pad - size, size);
    }

    private void drawBaseSquare(Graphics2D g2, PlayerColor c, int x, int y, int size) {
        Color col = awtColor(c);
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
        g2.fillRoundRect(x, y, size, size, 14, 14);
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 180));
        g2.drawRoundRect(x, y, size, size, 14, 14);
    }

    private void drawTokens(Graphics2D g2) {
        for (PlayerColor c : config.players()) {
            for (int i = 0; i < 4; i++) {
                TokenId t = new TokenId(c, i);
                TokenPosition pos = controller.position(t);
                Point p = pointForToken(t, pos);

                boolean movable = controller.isTokenMovableNow(t);

                Color col = awtColor(c);
                g2.setColor(col);
                g2.fillOval(p.x - tokenRadius, p.y - tokenRadius, tokenRadius * 2, tokenRadius * 2);

                g2.setColor(movable ? Color.BLACK : new Color(80, 80, 80));
                g2.setStroke(new BasicStroke(movable ? 3f : 1.5f));
                g2.drawOval(p.x - tokenRadius, p.y - tokenRadius, tokenRadius * 2, tokenRadius * 2);

                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                String label = String.valueOf(i + 1);
                g2.drawString(label, p.x - 3, p.y + 4);
            }
        }
    }

    private TokenId findTokenAt(Point mouse) {
        for (PlayerColor c : config.players()) {
            for (int i = 0; i < 4; i++) {
                TokenId t = new TokenId(c, i);
                TokenPosition pos = controller.position(t);
                Point p = pointForToken(t, pos);
                double dist = mouse.distance(p);
                if (dist <= tokenRadius + 3) {
                    return t;
                }
            }
        }
        return null;
    }

    private Point pointForToken(TokenId token, TokenPosition pos) {
        int w = getWidth();
        int h = getHeight();

        if (pos.type() == PositionType.START || pos.type() == PositionType.TRACK) {
            // Внутри Board позиция TRACK хранится как absIndex; START отрисовываем по стартовой клетке.
            int absTrackIndex = (pos.type() == PositionType.START) ? startIndex(token.color()) : pos.index();
            Point base = trackCellCenters.get(absTrackIndex);

            // смещение для двух фишек на одной клетке
            int offset = token.index() % 2 == 0 ? -8 : 8;
            return new Point(base.x + offset, base.y);
        }

        if (pos.type() == PositionType.HOME_LANE) {
            Point base = homeLaneCenters.get(token.color()).get(pos.index());
            int offset = token.index() % 2 == 0 ? -6 : 6;
            return new Point(base.x + offset, base.y);
        }

        if (pos.type() == PositionType.HOME) {
            Point base = new Point(w / 2, h / 2);
            int dx = (token.index() % 2 == 0) ? -10 : 10;
            int dy = (token.index() / 2 == 0) ? -10 : 10;
            return new Point(base.x + dx, base.y + dy);
        }

        // BASE
        return basePoint(token);
    }

    private Point basePoint(TokenId token) {
        int w = getWidth();
        int h = getHeight();

        int pad = 20;
        int size = 90;

        int x0;
        int y0;
        switch (token.color()) {
            case RED -> {
                x0 = pad;
                y0 = pad;
            }
            case BLUE -> {
                x0 = w - pad - size;
                y0 = pad;
            }
            case GREEN -> {
                x0 = w - pad - size;
                y0 = h - pad - size;
            }
            case YELLOW -> {
                x0 = pad;
                y0 = h - pad - size;
            }
            default -> {
                x0 = pad;
                y0 = pad;
            }
        }

        int col = token.index() % 2;
        int row = token.index() / 2;

        return new Point(x0 + 25 + col * 30, y0 + 25 + row * 30);
    }

    private int startIndex(PlayerColor color) {
        for (GameConfig.PlayerStart ps : config.starts()) {
            if (ps.color() == color) {
                return ps.startTrackIndex();
            }
        }
        return 0;
    }

    private Color awtColor(PlayerColor c) {
        return switch (c) {
            case RED -> new Color(220, 40, 40);
            case BLUE -> new Color(45, 110, 220);
            case GREEN -> new Color(30, 160, 90);
            case YELLOW -> new Color(240, 190, 40);
        };
    }
}

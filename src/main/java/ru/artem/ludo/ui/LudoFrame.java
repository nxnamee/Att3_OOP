package ru.artem.ludo.ui;

import ru.artem.ludo.core.GameConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Главное окно приложения Лудо.
 *
 * <p>Содержит панель поля, панель управления (бросок кубика/информация)
 * и связывает UI со состоянием игры.</p>
 */
public final class LudoFrame extends JFrame {

    private final GameConfig config;
    private final LudoController controller;

    /**
     * Основная панель отрисовки (поле + фишки).
     */
    private final BoardPanel boardPanel;

    /**
     * Метка со статусом: чей ход, что выпало, подсказки.
     */
    private final JLabel statusLabel;

    /**
     * Кнопка броска кубика.
     */
    private final JButton rollButton;

    /**
     * Создаёт окно игры.
     *
     * @param config конфигурация поля/игроков
     */
    public LudoFrame(GameConfig config) {
        super("Ludo");
        this.config = config;

        this.controller = new LudoController(config);
        this.boardPanel = new BoardPanel(config, controller);

        this.statusLabel = new JLabel("Готово");
        this.rollButton = new JButton("Бросить кубик");

        initUi();
        refreshFromModel();
    }

    private void initUi() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel controls = new JPanel(new BorderLayout(8, 8));
        controls.add(rollButton, BorderLayout.WEST);
        controls.add(statusLabel, BorderLayout.CENTER);

        rollButton.addActionListener(e -> {
            controller.roll();
            refreshFromModel();
        });

        root.add(boardPanel, BorderLayout.CENTER);
        root.add(controls, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    /**
     * Синхронизирует UI с текущим состоянием игры: перерисовка поля + статус.
     */
    public void refreshFromModel() {
        statusLabel.setText(controller.statusText());
        boardPanel.repaint();

        if (controller.isGameFinished()) {
            rollButton.setEnabled(false);
        }
    }
}

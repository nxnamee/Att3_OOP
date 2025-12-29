package ru.artem.ludo;

import ru.artem.ludo.core.GameConfig;
import ru.artem.ludo.ui.LudoFrame;

import javax.swing.*;

/**
 * Точка входа в приложение.
 *
 * <p>Запускает GUI-версию Лудо на Swing: поле отрисовывается в окне, бросок кубика
 * выполняется кнопкой, а ход выполняется кликом по фишке.</p>
 */
public final class App {

    /**
     * Запускает графический интерфейс.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        GameConfig config = GameConfig.defaultForFourPlayers();

        SwingUtilities.invokeLater(() -> {
            LudoFrame frame = new LudoFrame(config);
            frame.setVisible(true);
        });
    }

    private App() {
    }
}

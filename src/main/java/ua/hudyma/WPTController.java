package ua.hudyma;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Scanner;

public class WPTController extends JFrame {
    static JButton button;
    static JFrame jFrame;
    static JFileChooser fileChooser;
    static File file;
    JButtonListener buttonListener = new JButtonListener();

    public WPTController(String title) {
        super(title);
        jFrame = new JFrame();
        setLayout(new FlowLayout());
        button = new JButton("Відкрити WPT-file");
        add(button);
        button.addActionListener(buttonListener);
    }

    static class JButtonListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == button){
                fileChooser = new JFileChooser("s:/DOX/GPS/Waypoints");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Waypoint", "wpt"));
                int returnValue = fileChooser.showOpenDialog(jFrame);
                if (returnValue == JFileChooser.APPROVE_OPTION){
                    file = fileChooser.getSelectedFile();
                    try {
                        proceedSelectedFile (file);
                        JOptionPane.showMessageDialog(fileChooser, "Файл успішно збережено", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        System.exit(0);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(fileChooser, "Сталася халепа", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

        private void proceedSelectedFile(File file) throws IOException {
            var scanner = new Scanner(new FileReader(file));
            FileWriter newFile = new FileWriter("s:/newWPT.txt");
            while (scanner.hasNextLine()){
                String line = scanner.nextLine();
                line = line.replace(", 8,", ", 10,");
                newFile.write(line + System.lineSeparator());
            }
            newFile.close();
            scanner.close();
        }
    }
}

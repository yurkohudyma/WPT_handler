package ua.hudyma;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import static java.lang.System.err;
import static java.lang.System.exit;

public class WPTController extends JFrame {
    static JButton button, defButton;
    static JFrame jFrame;
    static JFileChooser fileChooser;
    static File gpxFile;
    static JButtonListener buttonListener = new JButtonListener();
    static JButtonListener defButtonListener = new JButtonListener();
    static String wptFileHeader = "OziExplorer Waypoint File Version 1.1\n" +
            "WGS 84\n" +
            "Reserved 2\n" +
            "garmin\n";
    static char comma = ',';

    public WPTController(String title) {
        super(title);
        jFrame = new JFrame();
        setLayout(new FlowLayout());
        button = new JButton("Відкрити GPX-file");
        defButton = new JButton("favorites.gpx");
        add(button);
        add(defButton);
        button.addActionListener(buttonListener);
        defButton.addActionListener(defButtonListener);
    }

    static class JButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == button) {
                fileChooser = new JFileChooser("s:/DOX/GPS/");
                fileChooser.setFileFilter(new FileNameExtensionFilter("GPX", "gpx"));
                int returnValue = fileChooser.showOpenDialog(jFrame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    gpxFile = fileChooser.getSelectedFile();
                    proceedConvertAndMessaging();
                }
            }
            else if (e.getSource() == defButton){
                gpxFile = new File("s:/DOX/GPS/favorites.gpx");
                proceedConvertAndMessaging();
            }
        }

        private void proceedConvertAndMessaging() {
            try {
                convertGPXtoWPTandWrite(gpxFile);
                JOptionPane.showMessageDialog(fileChooser, "Файл успішно збережено", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                exit(0);
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(fileChooser, "Файл не знайдено", "Помилка", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex){
                JOptionPane.showMessageDialog(fileChooser, "Сталася халепа", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
            catch (Exception ex){
                JOptionPane.showMessageDialog(fileChooser, "Проблема із записом файла", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void convertGPXtoWPTandWrite(File gpxFile) throws Exception {
            //gpx кодується в utf-8, а кирилицю у wpt ozi розпізнає коректно з win-1251
            FileWriter newFile = new FileWriter("s:/DOX/GPS/" + LocalDate.now() + "_TEST.wpt", Charset.forName("windows-1251"));
            var nodeList = loadDocument(gpxFile).getElementsByTagName("wpt");
            var wptLine = new StringBuilder(wptFileHeader);
            int node;
            String lon = "", lat = "", time = "", ele = "", desc = "", name = "";
            for (node = 0; node < nodeList.getLength(); node++) {
                var childNodeList = nodeList.item(node).getChildNodes();
                var attributeMap = nodeList.item(node).getAttributes();
                lon = attributeMap.getNamedItem("lon").getNodeValue();
                lat = attributeMap.getNamedItem("lat").getNodeValue();
                for (int childnode = 0; childnode < childNodeList.getLength(); childnode++) {
                    String nodeName = childNodeList.item(childnode).getNodeName();
                    switch (nodeName){
                        case "name" -> name = childNodeList.item(childnode).getTextContent();
                        case "ele" -> {
                            ele = childNodeList.item(childnode).getTextContent();
                            //gpx формат зберігає висоту в метрах, ozi читає їх у футах, тому вимушена подвійна конвертація
                            double feet = Double.parseDouble(ele) * 3.28084;
                            ele = (int) feet+"";
                        }
                        case "time" -> time = childNodeList.item(childnode).getTextContent();
                        case "cmt", "desc" -> desc = childNodeList.item(childnode).getTextContent();
                        default -> {}
                    }
                }
                //  Виправлення багу Ozi Explorer із нерозпізнанням великої кирилічної літери С, заміна на малу
                name = name.startsWith("С") ? name.replace('С', 'с') : name;
                wptLine.append(node + 1).append(comma).append(name).append(comma);
                wptLine.append("  ").append(lat).append(comma).append("  ").append(lon).append(comma);
                if (!time.isEmpty()) {
                    double timeinDays = convertDateToDays(time);
                    wptLine.append(timeinDays).append(comma);
                }
                else {
                    wptLine.append(comma);
                }
                wptLine.append("  0, 1, 3,         0,     65535,");
                if (!desc.isEmpty()){
                    wptLine.append(desc).append(comma);
                }
                else wptLine.append(comma);
                wptLine.append(" 0, 0,    0,");
                if (ele.isEmpty()) {
                    //ozi конвертує відсутню у gpx висоту в -777, що практично то саме, що не подати її взагалі, що я і роблю
                    wptLine.append(comma);
                }
                else {
                    wptLine.append("   ").append(ele).append(comma);
                }
                wptLine.append(" 8, 0,17,0,10.0,2,,,,120\n");
                newFile.write(wptLine.toString());
                wptLine = new StringBuilder();
                time = ""; ele = "";
                desc = ""; name = "";
            }
            newFile.close();
        }

        private double convertDateToDays(String time) {
            //дуже дивна точка початку відліку днів з початку століття. крім того, додається поправка на часовий пояс, бо тут час zulu
            //зазвичай береться 31-12-1899T00:00:00Z, видно тут якась мутка з австралійським часом
            LocalDateTime startDateTime = LocalDateTime.of(1899, 12, 30, 0, 0, 0);
            try {
                LocalDateTime userDateTime = LocalDateTime.parse(time.substring(0, time.length() - 1));
                long seconds = ChronoUnit.SECONDS.between(startDateTime, userDateTime);
                return ((double) seconds/86400);

            } catch (DateTimeParseException e) {
                err.println(e.getMessage());
                return -0d;
            }
        }

        private static Document loadDocument(File file) throws ParserConfigurationException, IOException, SAXException {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            return factory.newDocumentBuilder().parse(file);
        }
    }
}

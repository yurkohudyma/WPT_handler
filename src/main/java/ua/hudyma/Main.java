package ua.hudyma;

import javax.swing.*;
public class Main
{
    public static void main( String[] args )
    {
        WPTController wpt = new WPTController("Hudyma WPTproc.1");
        wpt.setVisible(true);
        wpt.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        wpt.setSize(180,105);
        wpt.setResizable(false);
        wpt.setLocationRelativeTo(null);
    }
}

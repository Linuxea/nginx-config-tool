package com.linuxea.nginx.script;

import java.io.IOException;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GUI {

  public static void main(String[] args) {
    JFrame frame = new JFrame("nginx file generator");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(400, 300);

    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);

    JLabel pkgLabel = new JLabel("Package:");
    JTextField pkgField = new JTextField(20);
    //default value
    pkgField.setText("com.linuxea.android");

    JLabel domainLabel = new JLabel("Domain:");
    JTextField domainField = new JTextField(20);
    //default value
    domainField.setText("linuxea.xyz");

    JLabel destLabel = new JLabel("Destination:");
    JTextField destField = new JTextField(20);
    //default value
    destField.setText("./");

    JButton button = new JButton("Click me!");
    button.addActionListener(e -> {
      long start = System.currentTimeMillis();
      String pkg = pkgField.getText();
      String domain = domainField.getText();
      String dest = destField.getText();
      ScriptUtil scriptUtil = new ScriptUtil();
      try {
        scriptUtil.generate(pkg, domain, dest);
      } catch (IOException | InterruptedException ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
      long count = System.currentTimeMillis() - start;
      JOptionPane.showMessageDialog(frame, "Done!" + "cost time: " + count / 1000 + "s", "Success",
          JOptionPane.INFORMATION_MESSAGE);
    });

    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
    hGroup.addGroup(layout.createParallelGroup().addComponent(pkgLabel).addComponent(domainLabel)
        .addComponent(destLabel).addComponent(button));
    hGroup.addGroup(layout.createParallelGroup().addComponent(pkgField).addComponent(domainField)
        .addComponent(destField));
    layout.setHorizontalGroup(hGroup);

    GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
    vGroup.addGroup(
        layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(pkgLabel)
            .addComponent(pkgField));
    vGroup.addGroup(
        layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(domainLabel)
            .addComponent(domainField));
    vGroup.addGroup(
        layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(destLabel)
            .addComponent(destField));
    vGroup.addGroup(
        layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(button));
    layout.setVerticalGroup(vGroup);

    frame.add(panel);
    frame.setResizable(false);
    frame.setVisible(true);
  }
}



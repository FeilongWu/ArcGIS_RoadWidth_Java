package net.mapsay.polygon.service;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;


public class GUI {

    private static String path;
    private static  int upStatus=0; // file upload status, 0=no file, 1=uploaded
    private static  Icon pressIcon = new Icon() {
        int borderWidth=3;
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(Color.red);
            g.fillRect(x + borderWidth, y + borderWidth, getIconWidth() - 2 * borderWidth,
                    getIconHeight() - 2 * borderWidth);
        }

        @Override
        public int getIconWidth() {
            return 10;
        }

        @Override
        public int getIconHeight() {
            return 0;
        }
    };

public static void  setText (JTextArea textArea,String string){
    textArea.setText(string);
}

    public static void main(String args[]) {
        JFrame frame = new JFrame("道路宽度计算程序");
        JTextArea textBox = new JTextArea();
        textBox.setFont(new Font("Serif",Font.PLAIN,18));
        textBox.setText("上传一个.shp文件然后点击下面'开始'键"+"\n"+"进行运算。");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setMinimum(0);progressBar.setMaximum(100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(340, 200);
        JDialog dialog = new JDialog(frame,"道路宽度计算");

        dialog.add(progressBar);
        dialog.setSize(new Dimension(90,90));
        JButton button=new JButton("开始");


        button.setPreferredSize(new Dimension(40, 40));
        button.setPressedIcon(pressIcon);
        button.setHorizontalTextPosition(JButton.LEFT);
        button.setBorder(BorderFactory.createEtchedBorder());
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e3) {
                if (upStatus == 1) {


                        try {
                            setText(textBox,"正在计算...");


                            java.awt.EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        RoadWidthService.main(path);
                                        java.awt.EventQueue.invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                textBox.setText("计算完毕。");
                                            }
                                        });
                                    }catch (Exception ex){System.out.println("无法对此文件进行计算。");}
                                }
                            });


                        }catch (Exception e1){textBox.setText("错误...");}
                    }

            }
        });


            JMenuBar menuBar = new JMenuBar();
            JMenu Menu = new JMenu("文件");
            menuBar.add(Menu);


            JMenuItem menuItem = new JMenuItem(new AbstractAction("打开") {
                public void actionPerformed(ActionEvent e) {
                    String[] fileName;
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(".shp", "shp");
                    JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getDefaultDirectory());
                    fileChooser.setFileFilter(filter);
                    int returnValue = fileChooser.showOpenDialog(null);
                    if (returnValue == fileChooser.APPROVE_OPTION) {
                        File selelectedF = fileChooser.getSelectedFile();
                        upStatus = 1;
                        path = selelectedF.getAbsolutePath();
                        fileName=path.split("\\\\");
                        textBox.setText("已选择以下文件：\n"+fileName[fileName.length-1 ]);




                    }
                }

            });

            Menu.add(menuItem);
            //add elements to frame
            frame.add(BorderLayout.NORTH, menuBar);
            frame.add(BorderLayout.SOUTH,button);
            frame.add(BorderLayout.CENTER,textBox);


            frame.setVisible(true);


    }
}




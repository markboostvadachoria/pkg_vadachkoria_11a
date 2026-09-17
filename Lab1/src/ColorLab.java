import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

public class ColorLab extends JPanel {
    final ModelPanel[] panels=new ModelPanel[3];
    final JLabel status=new JLabel(" "), hex=new JLabel();
    final JPanel sample=new JPanel();
    boolean updating;
    public ColorLab() {
        setLayout(new BorderLayout(5,5)); setBorder(new EmptyBorder(5,5,5,5));setPreferredSize(new Dimension(900,560));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Текущий цвет:"));
        sample.setPreferredSize(new Dimension(75,30)); sample.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        top.add(sample); top.add(hex); add(top,BorderLayout.NORTH);
        JPanel models=new JPanel(new GridLayout(1,3,5,0));
        panels[0]=new ModelPanel(0,"CMYK",new String[]{"C, %","M, %","Y, %","K, %"},new double[]{0,0,0,0},new double[]{100,100,100,100});
        panels[1]=new ModelPanel(1,"LAB",new String[]{"L*","a*","b*"},new double[]{0,-128,-128},new double[]{100,127,127});
        panels[2]=new ModelPanel(2,"HSV",new String[]{"H, °","S, %","V, %"},new double[]{0,0,0},new double[]{360,100,100});
        for(ModelPanel p:panels) models.add(p); add(models,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new GridLayout(2,1,0,2));
        bottom.add(status);
        bottom.add(new JLabel("Ввод — Enter. Палитра — нажать или перетащить мышью. X и Y задают оси палитры."));
        add(bottom,BorderLayout.SOUTH); updateColor(-1,new double[]{.22,.48,.86});
    }
    void updateColor(int source,double[] raw) {
        boolean clipped=ColorModels.outOfGamut(raw); double[] rgb=ColorModels.clip(raw);
        updating=true;
        for(ModelPanel p:panels) { if(p.id!=source) p.values=ColorModels.to(p.id,rgb); p.sync(); }
        updating=false;
        Color color=new Color((float)rgb[0],(float)rgb[1],(float)rgb[2]); sample.setBackground(color);
        hex.setText(String.format("#%02X%02X%02X",color.getRed(),color.getGreen(),color.getBlue()));
        status.setForeground(Color.BLACK);
        status.setText(clipped?"Цвет обрезан до sRGB. Исходные LAB сохранены, CMYK и HSV пересчитаны по экранному цвету.":" ");
    }
    class ModelPanel extends JPanel {
        final int id;
        final String[] names;
        final double[] min,max;
        double[] values;
        final JTextField[] fields;
        final JSlider[] sliders;
        final JComboBox<String> axisX,axisY;
        final Palette palette;
        ModelPanel(int id,String title,String[] names,double[] min,double[] max) {
            this.id=id;this.names=names;this.min=min;this.max=max;values=new double[names.length];
            fields=new JTextField[names.length];sliders=new JSlider[names.length];
            setLayout(new BorderLayout(5,5));setBorder(BorderFactory.createTitledBorder(title));
            JPanel controls=new JPanel(new GridLayout(4,1,0,3));
            for(int i=0;i<names.length;i++) {
                final int n=i; JPanel row=new JPanel(new BorderLayout(5,2));
                JPanel line=new JPanel(new BorderLayout());
                line.add(new JLabel(names[i]+" ["+(int)min[i]+"…"+(int)max[i]+"]"),BorderLayout.WEST);

                fields[i]=new JTextField(8);
                line.add(fields[i],BorderLayout.EAST);
                row.add(line,BorderLayout.NORTH);

                sliders[i]=new JSlider(0,10000);
                row.add(sliders[i],BorderLayout.SOUTH);
                controls.add(row);
                fields[i].addActionListener(e->commit(n));
                fields[i].addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){commit(n);}});
                sliders[i].addChangeListener(e-> {
                    if(!updating)
                    {
                        values[n]=min[n] + sliders[n].getValue() / 10000.0 * (max[n]-min[n]);
                        updateColor(id,ColorModels.from(id,values));
                    }
                });
            }
            while(controls.getComponentCount()<4)
                controls.add(new JPanel());
            add(controls,BorderLayout.CENTER);
            JPanel lower=new JPanel(new BorderLayout(4,8));
            JPanel axes=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
            axisX=new JComboBox<>(names);axisY=new JComboBox<>(names);
            axisX.setSelectedIndex(id==1?1:0);axisY.setSelectedIndex(id==1?2:1);
            axes.add(new JLabel("X:"));axes.add(axisX);
            axes.add(new JLabel("Y:"));axes.add(axisY);
            lower.add(axes,BorderLayout.NORTH);
            palette=new Palette();
            palette.setPreferredSize(new Dimension(240,140));
            lower.add(palette,BorderLayout.CENTER);
            axisX.addActionListener(e->axesChanged(true));
            axisY.addActionListener(e->axesChanged(false));
            lower.add(new JLabel(id==1?"Штриховка — вне sRGB":" "),BorderLayout.SOUTH);
            add(lower,BorderLayout.SOUTH);
        }
        void axesChanged(boolean x) {
            if(axisX.getSelectedIndex()==axisY.getSelectedIndex()){
                if(x)axisY.setSelectedIndex((axisX.getSelectedIndex() + 1) % names.length);
                else axisX.setSelectedIndex((axisY.getSelectedIndex() + 1) % names.length);
            }palette.invalidateImage();
        }
        void commit(int n) {
            if(updating)
                return;
            try {
                double v=Double.parseDouble(fields[n].getText().trim().replace(',','.'));
                if(!Double.isFinite(v)||v<min[n]||v>max[n]) throw new NumberFormatException();
                if(fields[n].getText().equals(format(values[n])))
                    return;
                values[n]=v;
                updateColor(id,ColorModels.from(id,values));
            }catch(NumberFormatException ex){
                fields[n].setText(format(values[n]));
                status.setForeground(new Color(160,60,0));
                status.setText("Некорректный ввод: " + names[n] + " должен быть числом от " + min[n] + " до " + max[n] + ". Прежнее значение восстановлено.");}
        }
        void sync(){
            for(int i=0;i<names.length;i++){
                fields[i].setText(format(values[i]));
                sliders[i].setValue((int)Math.round((values[i]-min[i]) / (max[i]-min[i])*10000));
            }
            palette.invalidateImage();
        }
        class Palette extends JPanel {
            BufferedImage cache;
            Palette(){
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                MouseAdapter mouse=new MouseAdapter(){
                    public void mousePressed(MouseEvent e){
                        pick(e);
                    }
                    public void mouseDragged(MouseEvent e){
                        pick(e);
                    }
                };
                addMouseListener(mouse);
                addMouseMotionListener(mouse);
            }
            void invalidateImage(){
                cache=null;
                repaint();
            }
            void pick(MouseEvent e){
                int a=axisX.getSelectedIndex(),b=axisY.getSelectedIndex();
                values[a]=min[a]+ColorModels.clamp(e.getX()/(double)Math.max(1,getWidth()-1))*(max[a]-min[a]);
                values[b]=min[b]+(1-ColorModels.clamp(e.getY()/(double)Math.max(1,getHeight()-1)))*(max[b]-min[b]);
                updateColor(id,ColorModels.from(id,values));
            }
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                int w=getWidth(),h=getHeight();
                if(w<2||h<2)
                    return;
                int a=axisX.getSelectedIndex(),b=axisY.getSelectedIndex();
                if(cache==null||cache.getWidth()!=w||cache.getHeight()!=h){
                    cache=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
                    double[] v=values.clone();
                    for(int y=0;y<h;y++)
                        for(int x=0;x<w;x++){
                            v[a]=min[a]+x/(double)(w-1)*(max[a]-min[a]);
                            v[b]=min[b]+(1-y/(double)(h-1))*(max[b]-min[b]);
                            double[] raw=ColorModels.from(id,v),rgb=ColorModels.clip(raw);
                            Color c=new Color((float)rgb[0],(float)rgb[1],(float)rgb[2]);
                            if(ColorModels.outOfGamut(raw)&&(x+y)%12<2)
                                c=c.darker();
                            cache.setRGB(x,y,c.getRGB());
                        }
                }
                g.drawImage(cache,0,0,null);
                int x=(int)((values[a]-min[a])/(max[a]-min[a])*(w-1)),y=(int)((1-(values[b]-min[b])/(max[b]-min[b]))*(h-1));g.setColor(Color.BLACK);
                g.drawOval(x-5,y-5,10,10);g.setColor(Color.WHITE);
                g.drawOval(x-4,y-4,8,8);
            }
        }
    }
    static String format(double v){
        return String.format(Locale.ROOT,"%.3f",v);
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{JFrame frame=new JFrame("Лабораторная 1 — вариант 5");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new ColorLab());
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

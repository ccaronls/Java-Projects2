package cc.app.fractal;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import cc.app.fractal.FractalComponent.FractalImage;
import cc.app.fractal.evaluator.*;
import cc.lib.math.ComplexNumber;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTPanel;
import cc.lib.utils.FileUtils;

public class FractalViewer extends AWTFrame implements FractalComponent.FractalListener, ActionListener{

    static final String PROP_CURRENT_DIR = "CURRENT_DIRECTORY";
    static final String PROP_ANIM_START_FIELD = "ANIM_START_FIELD";
    static final String PROP_ANIM_END_FIELD = "ANIM_END_FIELD";
    static final String PROP_CONSTANT_EXPRESSION = "CONSTANT_EXPRESSION";
    static final String PROP_COLOR_SCALE = "COLOR_SCALE";
    static final String PROP_ZOOM_LEFT = "ZOOM_LEFT";
    static final String PROP_ZOOM_RIGHT = "ZOOM_RIGHT";
    static final String PROP_ZOOM_TOP = "ZOOM_TOP";
    static final String PROP_ZOOM_BOTTOM = "ZOOM_BOTTOM";
    static final String PROP_FRACTAL_SET = "FRACTAL_SET";
    static final String PROP_ANIM_NUM_FRAMES = "ANIM_NUM_FRAMES";
    static final String PROP_SHOW_WATERMARK_BOOLEAN = "SHOW_WATERMARK";

    static final String [] frameOptions = { "100", "150", "200", "250", "300", "400", "500"};


    public static void main(String [] args) {
        new FractalViewer();       
    }
    
    final int DEFAULT_ZOOM = 2;
    
    enum Action {
        UNDO("Undo"),
        REDO("Redo"),
        ZOOMIN("Zoom in"),
        ZOOMOUT("Zoom out"),
        SAVE("Save"),
        CENTER("Center"),
        SET_SCALE(""),
        INVERT_COLORS("Invert"),
        BRIGHTEN_COLORS("Brighten"),
        DARKEN_COLORS("Darken"),
        ROTATE_COLORS("Rotate"),
        MANDELBROT_SET("Mandelbrot Set"),
        JULIA_SET("Julia Set"),
        ANIM_CONSTANT_START("Start"),
        ANIM_CONSTANT_END("End"),
        CUSTOM_SET_EXPRESSION("Zi="),
        CUSTOM_SET("Custom"),
        CUSTOM_SET_CONSTANT("C="),
        ANIMATE("Animate"),
        CANCEL("Cancel"),  
        SHOW_WATERMARK("Water Mark"),
        MAKE_MOVIE("Make Movie"),
        POSITION("Position"),
        ;
        
        private Action(String label) {
            this.label = label;
        }
        
        private final String label;
        
        public String getLabel() {
            return this.label;
        }
    }

    JPanel leftButtons = new JPanel();
    JPanel rightButtons = new JPanel();
    FractalComponent fractalComponent;
    JProgressBar progressBar = new JProgressBar(0, 100);
    JComboBox formulaExpression;
    JComboBox animationNumFrames;
    JTextField constantExpression;
    AbstractButton undoButton;
    AbstractButton redoButton;
    AbstractButton saveButton;
    AbstractButton animateButton;
    AbstractButton zoomInButton;
    AbstractButton zoomOutButton;
    AbstractButton cancelButton;
    AbstractButton showWatermarkButton;
    JTextField animStartField;
    JTextField animEndField;
    AnimateThread animation;
    JTextField zoomLeft, zoomRight, zoomTop, zoomBottom;
//    final HashMap<String, ComplexNumber> vars = new HashMap<String, ComplexNumber>();
    final AEvaluator evaluator = new Evaluator();
    
    Vector<String> formulas = new Vector<String>();

    final File FORMULAS_FILE;
    final File ANIMS_DIR;
    final File MOVIES_DIR;
    
    FractalViewer() {
        /*
        vars.put("E", new ComplexNumber(Math.E, 0));
        vars.put("PI", new ComplexNumber(Math.PI, 0));
        vars.put("A", new ComplexNumber());
        vars.put("B", new ComplexNumber());
        vars.put("C", new ComplexNumber());
        */
        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        setPropertiesFile(new File(settings, "fractal.properties"));
        FORMULAS_FILE = new File(settings, "formulas.txt");
        ANIMS_DIR = new File(settings, "/anims");
        if (!ANIMS_DIR.isDirectory()) {
            if (!ANIMS_DIR.mkdir())
                throw new RuntimeException("Failed to create ANIMS_DIR: " + ANIMS_DIR);
        }
        MOVIES_DIR = new File(System.getProperty("user.home") + "/Documents/Movies");
        if (!MOVIES_DIR.isDirectory()) {
            if (!MOVIES_DIR.mkdir()) {
                throw new RuntimeException("Failed to create MOVIES_DIR: " + MOVIES_DIR);
            }
        }
        listScreens();
        addWindowListener(this);
        
        loadFormulas();
        
        ButtonGroup group;
        JPanel panel;
        
        leftButtons.setLayout(new GridLayout(0, 2));
        rightButtons.setLayout(new GridLayout(15, 1));
        
        add(leftButtons, BorderLayout.WEST);
        add(rightButtons, BorderLayout.EAST);
        undoButton = addButton(Action.UNDO, leftButtons, false);
        redoButton = addButton(Action.REDO, leftButtons, false);
        zoomInButton = addButton(Action.ZOOMIN, leftButtons);
        zoomOutButton = addButton(Action.ZOOMOUT, leftButtons);

        addButton(Action.SAVE, leftButtons);
        addButton(Action.CENTER, leftButtons);

        JPanel mv1 = new AWTPanel(0, 1);
        zoomLeft = addTextField(Action.POSITION, "LEFT", mv1, getStringProperty(PROP_ZOOM_LEFT, "-2"));
        zoomRight = addTextField(Action.POSITION, "RIGHT", mv1, getStringProperty(PROP_ZOOM_RIGHT, "2"));
        leftButtons.add(mv1);
        mv1 = new AWTPanel(0, 1);
        zoomTop = addTextField(Action.POSITION, "TOP", mv1, getStringProperty(PROP_ZOOM_TOP, "-2"));
        zoomBottom = addTextField(Action.POSITION, "BOTTOM", mv1, getStringProperty(PROP_ZOOM_BOTTOM, "2"));
        leftButtons.add(mv1);

        group = new ButtonGroup();
        
        String fractalSetString = getStringProperty(PROP_FRACTAL_SET, Action.MANDELBROT_SET.name());
        Action fractalSet = Action.MANDELBROT_SET;
        try {
            fractalSet = Action.valueOf(fractalSetString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        addRadioButton(Action.MANDELBROT_SET, leftButtons, group, fractalSet == Action.MANDELBROT_SET);
        leftButtons.add(new JLabel("Z(i+1) = Zi^2 + Z0"));
        
        addRadioButton(Action.JULIA_SET, leftButtons, group, fractalSet == Action.JULIA_SET);
        leftButtons.add(new JLabel("Z(i+1) = Zi^2 - C"));
        
        addRadioButton(Action.CUSTOM_SET, leftButtons, group, fractalSet == Action.CUSTOM_SET);
        leftButtons.add(new JPanel()); // empty space
        JComboBox combo = new JComboBox(formulas);
        combo.setEditable(true);
        combo.addActionListener(this);
        combo.setMaximumSize(new Dimension(100, combo.getMaximumSize().height));
        combo.setActionCommand(Action.CUSTOM_SET_EXPRESSION.name());
        
        formulaExpression = combo;
        formulaExpression.setEnabled(false);
        panel= new JPanel();
        panel.add(new JLabel(Action.CUSTOM_SET_EXPRESSION.getLabel()));
        panel.add(formulaExpression);
        leftButtons.add(panel);
        
        //addRadioButton(Action.CUSTOM_SET_JULIA_MODE, leftButtons, group, fractalSet == Action.CUSTOM_SET_JULIA_MODE);
        constantExpression = addTextField(Action.CUSTOM_SET_CONSTANT, leftButtons, getStringProperty(PROP_CONSTANT_EXPRESSION, "[0.5,0.5]"));
        constantExpression.setEnabled(false);
        
        animStartField = addTextField(Action.ANIM_CONSTANT_START, leftButtons, this.getStringProperty(PROP_ANIM_START_FIELD, "[0,0]"));
        animStartField.setEnabled(false);
        animEndField = addTextField(Action.ANIM_CONSTANT_END, leftButtons, getStringProperty(PROP_ANIM_END_FIELD, "[1,1]"));
        animEndField.setEnabled(false);
        
        animateButton = addButton(Action.ANIMATE, leftButtons, false);
        cancelButton = addButton(Action.CANCEL, leftButtons);
        
        animationNumFrames = new JComboBox(frameOptions);
        animationNumFrames.setSelectedItem(getStringProperty(PROP_ANIM_NUM_FRAMES, frameOptions[0]));

        panel = new JPanel();
        panel.add(new JLabel("Animation num frames"));
        panel.add(animationNumFrames);
        leftButtons.add(panel);
        showWatermarkButton = addButton(new JToggleButton(Action.SHOW_WATERMARK.getLabel()), Action.SHOW_WATERMARK, leftButtons, null);
        showWatermarkButton.setSelected(getBooleanProperty(PROP_SHOW_WATERMARK_BOOLEAN, false));
        
        String scaleString = getStringProperty(PROP_COLOR_SCALE, ColorTable.Scale.RAINBOW_SCALE.name());
        ColorTable.Scale scale = ColorTable.Scale.valueOf(scaleString);
        ColorTable colorTable = new ColorTable(scale);

        group = new ButtonGroup();
        for (ColorTable.Scale s : ColorTable.Scale.values()) {
            addRadioButton(Action.SET_SCALE, s.name(), rightButtons, group, s == scale);
        }
        addButton(Action.INVERT_COLORS, rightButtons);
        addButton(Action.BRIGHTEN_COLORS, rightButtons);
        addButton(Action.DARKEN_COLORS, rightButtons);
        //addButton(Action.ROTATE_COLORS, rightButtons);
        {
        	Button b = new Button("Rotate");
        	b.addMouseListener(new MouseButtonListener() {
				
				@Override
				protected void doAction() {
					fractalComponent.getColorTable().rotateColors();
                    fractalComponent.startNewFractal(true);
				}
			});
            rightButtons.add(b, null);
        }

        addButton(Action.MAKE_MOVIE, rightButtons);

        fractalComponent = new FractalComponent(colorTable, 2);
        fractalComponent.setShowWatermark(showWatermarkButton.isSelected());
        
        try {
            switch (fractalSet) {
                case JULIA_SET: {
                    evaluator.parse(constantExpression.getText());
                    ComplexNumber constant = evaluator.evaluate();
                    fractalComponent.setFractal(constant, new AFractal.Julia());
                    constantExpression.setEnabled(true);
                    animStartField.setEnabled(true);
                    animEndField.setEnabled(true);
                    animateButton.setEnabled(true);
                    break;
                }
                case CUSTOM_SET: {
                	ComplexNumber constant = null;
                	try {
                		evaluator.parse(constantExpression.getText());
                		constant = evaluator.evaluate();
                	} catch (TokenMgrError e) {
                		e.printStackTrace();
                		constant = new ComplexNumber();
                	}
                    //ComplexNumber constant = evaluator.evaluate();
                    fractalComponent.setFractal(constant, new AFractal.Custom(getExpressionText()));
                    constantExpression.setEnabled(true);
                    formulaExpression.setEnabled(true);
                    animStartField.setEnabled(true);
                    animEndField.setEnabled(true);
                    animateButton.setEnabled(true);
                    break;
                }
                
                case MANDELBROT_SET:
                default:
                    fractalComponent.setFractal(new ComplexNumber(), new AFractal.Mandelbrot()); 
                    break;
                
            }
        } catch (Exception e) {
            fractalComponent.setFractal(new ComplexNumber(), new AFractal.Mandelbrot());
            e.printStackTrace();
        }
        double zoomLeft = getDoubleProperty(PROP_ZOOM_LEFT, -DEFAULT_ZOOM);
        double zoomRight = getDoubleProperty(PROP_ZOOM_RIGHT, DEFAULT_ZOOM);
        double zoomTop = getDoubleProperty(PROP_ZOOM_TOP, DEFAULT_ZOOM);
        double zoomBottom = getDoubleProperty(PROP_ZOOM_BOTTOM, DEFAULT_ZOOM);
        fractalComponent.zoomRect(zoomLeft, zoomRight, zoomTop, zoomBottom);
        add(fractalComponent);
        animation = new AnimateThread(this.fractalComponent, this, constantExpression);
        
        fractalComponent.setFractalListener(this);
        JPanel progressLayout = new JPanel(new BorderLayout());
        addButton(Action.CANCEL, progressLayout, BorderLayout.EAST);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressLayout.add(progressBar);
        add(progressLayout, BorderLayout.SOUTH);

        if (!restoreFromProperties()) {
            centerToScreen(640, 480);
        }
    }
    
    @Override
    public void onProgress(int progress) {
        this.progressBar.setValue(progress);
        this.progressBar.repaint();
        synchronized (this) {
            notifyAll();
        }
    }
    
    @Override
    public void onDone() {
        System.out.println("onDone");
        undoButton.setEnabled(fractalComponent.canUndo());
        redoButton.setEnabled(fractalComponent.canRedo());
        setProperty(PROP_CONSTANT_EXPRESSION, constantExpression.getText());
    }
    
    public void onAnimationDone(AnimateThread animateThread) {
        setProperty(PROP_CONSTANT_EXPRESSION, constantExpression.getText());
        this.animateButton.setText(Action.ANIMATE.getLabel());
        this.animateButton.setEnabled(true);
        if (!animateThread.isCancelled()) {
            makeMovie();
        }
    }

    public void makeMovie() {
        String movieName = "fractalAnim" + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".mp4";
        File movieFile = new File(MOVIES_DIR, movieName);
        String cmd = "/usr/local/bin/ffmpeg -y -r 30 -i " + ANIMS_DIR.getAbsolutePath() + "/anim%03d.png -vcodec libx264 -crf 10  -pix_fmt rgb24 " + movieFile;
//        String cmd = "/usr/local/bin/ffmpeg -y -r 30 -i " + ANIMS_DIR.getAbsolutePath() + "/anim%03d.png -pix_fmt rgb24 " + movieFile;
        System.out.println("Making movie with CMD: " + cmd);
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            if (p.waitFor() == 0) {
                Runtime.getRuntime().exec("/usr/bin/open " + movieFile.getAbsolutePath());
            } else {
                System.err.println(FileUtils.inputStreamToString(p.getErrorStream()));
                System.out.println(FileUtils.inputStreamToString(p.getInputStream()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "ERROR:" + e.getClass().getSimpleName() + " " + e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
    }
    
    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, "ERROR:" + msg, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    AbstractButton addButton(Action action, JPanel panel, boolean enabled) {
        AbstractButton b = addButton(new JButton(action.getLabel()), action, panel, null);
        b.setEnabled(enabled);
        return b;
    }

    AbstractButton addButton(Action action, JPanel panel) {
        return addButton(new JButton(action.getLabel()), action, panel, null);
    }

    void addRadioButton(Action action, JPanel panel, ButtonGroup group) {
        addRadioButton(action, panel, group, false);
    }

    void addRadioButton(Action action, JPanel panel, ButtonGroup group, boolean chosen) {
        addRadioButton(action, action.getLabel(), panel, group, chosen);
    }
    
    void addRadioButton(Action action, String label, JPanel panel, ButtonGroup group, boolean chosen) {
        JRadioButton button = new JRadioButton(label);
        group.add(button);
        button.setSelected(chosen);
        addButton(button, action, panel, null);
    }

    AbstractButton addButton(Action action, JPanel panel, Object extra) {
        return addButton(new JButton(action.getLabel()), action, panel, null);
    }

    AbstractButton addButton(AbstractButton button, Action action, JPanel panel, Object extra) {
        button.setActionCommand(action.name());
        button.addActionListener(this);
        if (extra != null)
            panel.add(button, extra);
        else
            panel.add(button);
        return button;
    }
    JTextField addTextField(Action fAction, JPanel panel, String defaultText) {
        return addTextField(fAction, fAction.getLabel(), panel, defaultText);
    }

    JTextField addTextField(Action fAction, String label, JPanel panel, String defaultText) {
        JTextField input = new JTextField(10);
        JPanel container = new JPanel();
        input.setText(defaultText);
        input.setActionCommand(fAction.name());
        input.addActionListener(this);
        container.add(new JLabel(label));
        container.add(input);
        panel.add(container);
        return input;
    }
    
    String getExpressionText() {
        Object select = formulaExpression.getSelectedItem();
        if (select == null)
            return formulas.get(0);
        return (String)select;
    }

    void saveSettings() {
        FractalImage i = fractalComponent.getLastFractalImage();
        setProperty(PROP_ZOOM_LEFT, String.valueOf(i.left));
        setProperty(PROP_ZOOM_RIGHT, String.valueOf(i.right));
        setProperty(PROP_ZOOM_TOP, String.valueOf(i.top));
        setProperty(PROP_ZOOM_BOTTOM, String.valueOf(i.bottom));
        setProperty(PROP_ANIM_START_FIELD, animStartField.getText());
        setProperty(PROP_ANIM_END_FIELD, animEndField.getText());
        setProperty(PROP_ANIM_NUM_FRAMES, animationNumFrames.getSelectedItem().toString());
        setProperty(PROP_CONSTANT_EXPRESSION, constantExpression.getText());
    }
    
    @Override
    public void actionPerformed(final ActionEvent ac) {
        try {
            String command = ac.getActionCommand();
            System.out.println("Process command: " + command);
            if (command.equals("comboBoxEdited"))
                command = Action.CUSTOM_SET_EXPRESSION.name();
            Action a = Action.valueOf(command);
            switch (a) {
                case UNDO:
                    fractalComponent.undo();
                    onDone();
                    break;
                    
                case REDO:
                    fractalComponent.redo();
                    onDone();
                    break;
                    
                case ZOOMIN:
                    fractalComponent.zoom(0.5f);
                    break;
                    
                case ZOOMOUT:
                    fractalComponent.zoom(2f);
                    break;
                    
                case CENTER:
                    fractalComponent.zoomRect(-DEFAULT_ZOOM, DEFAULT_ZOOM, DEFAULT_ZOOM, -DEFAULT_ZOOM);
                    break;

                case POSITION: {
                    double left = Double.parseDouble(zoomLeft.getText());
                    double right = Double.parseDouble(zoomRight.getText());
                    double top = Double.parseDouble(zoomTop.getText());
                    double bottom = Double.parseDouble(zoomBottom.getText());
                    fractalComponent.zoomRect(left, right, top, bottom);
                    break;
                }
                    
                case SAVE: {

                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Save PNG File");
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "Images", "png", "gif", "jpg", "jpeg");
                    chooser.setFileFilter(filter);
                    chooser.setCurrentDirectory(new File(getStringProperty(PROP_CURRENT_DIR, "")));
                    int returnVal = chooser.showSaveDialog(this);
                    if(returnVal == JFileChooser.APPROVE_OPTION) {
                        
                        String fileName = chooser.getSelectedFile().getName();
                        String format = "png";
                        int dot = fileName.lastIndexOf('.');
                        if (dot < 0) {
                            fileName += "." + format;
                        } else {
                            format = fileName.substring(dot+1).toLowerCase();
                        }
                        
                        File file = new File(chooser.getCurrentDirectory(), fileName);
                        if (!file.exists() || JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "File Exists.  Overwrite?")) {
                            setProperty(PROP_CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());
                            fractalComponent.saveImage(file, format);
                        } 
                    }
                    break;
                }
                case ANIMATE: {
                    if (animation.isAnimating()) {
                        if (animation.isPaused()) {
                            animateButton.setText("Pause");
                            animation.resume();
                        } else {
                            animateButton.setText("Resume");
                            animation.pause();
                        }
                    } else {
                        saveSettings();
                        evaluator.parse(this.animStartField.getText());
                        ComplexNumber start = new ComplexNumber(evaluator.evaluate());
                        evaluator.parse(this.animEndField.getText());
                        ComplexNumber end   = new ComplexNumber(evaluator.evaluate());
                        animateButton.setText("Pause");
                        File dir = ANIMS_DIR;
                        if (!dir.isDirectory()) {
                            if (dir.exists()) {
                                System.err.println("anims dir: " + dir.getAbsolutePath() + " is not a directory");
                                dir = null;
                            } else if (!dir.mkdir()) {
                                System.err.println("Cannot create directory: " + dir.getAbsolutePath());
                                dir = null;
                            }
                        } else {
                        	// purge directory
                        	File [] files = dir.listFiles();
                        	for (File f : files) {
                        		f.delete();
                        	}
                        }
                        int numFrames = Integer.parseInt(animationNumFrames.getSelectedItem().toString());
                        System.out.println("Num frames = numFrames");
                        animation.startAnimation(dir, start, end, numFrames);
                    }
                    break;
                }
                
                case SET_SCALE: {
                    ColorTable.Scale scale = ColorTable.Scale.valueOf(((AbstractButton)ac.getSource()).getText());
                    fractalComponent.getColorTable().setScale(scale);
                    fractalComponent.startNewFractal(true);
                    setProperty(PROP_COLOR_SCALE, scale.name());
                    break;
                }
                    
                case MANDELBROT_SET:
                    constantExpression.setEnabled(false);
                    formulaExpression.setEnabled(false);
                    animStartField.setEnabled(false);
                    animEndField.setEnabled(false);
                    animateButton.setEnabled(false);
                    fractalComponent.setFractal(new ComplexNumber(), new AFractal.Mandelbrot());
                    fractalComponent.reset(true);
                    fractalComponent.startNewFractal(false);
                    setProperty(PROP_FRACTAL_SET, a.name());
                    break;
                    
                case JULIA_SET: {
                    constantExpression.setEnabled(true);
                    formulaExpression.setEnabled(false);
                    animStartField.setEnabled(true);
                    animEndField.setEnabled(true);
                    animateButton.setEnabled(true);
                    evaluator.parse(constantExpression.getText());
                    fractalComponent.setFractal(evaluator.evaluate(), new AFractal.Julia());
                    fractalComponent.reset(true);
                    fractalComponent.startNewFractal(false);
                    setProperty(PROP_FRACTAL_SET, a.name());
                    break;
                }
                    
                case CUSTOM_SET:
                    constantExpression.setEnabled(true);
                    formulaExpression.setEnabled(true);
                    animStartField.setEnabled(true);
                    animEndField.setEnabled(true);
                    animateButton.setEnabled(true);
                    evaluator.parse(constantExpression.getText());
                    ComplexNumber c = evaluator.evaluate();
                    fractalComponent.setFractal(c, new AFractal.Custom(getExpressionText()));
                    fractalComponent.reset(true);
                    fractalComponent.startNewFractal(false);
                    setProperty(PROP_FRACTAL_SET, a.name());
                    break;
                    
                case CUSTOM_SET_EXPRESSION: {
                    evaluator.parse(this.constantExpression.getText());
                    fractalComponent.setFractal(evaluator.evaluate(), new AFractal.Custom(getExpressionText()));
                    fractalComponent.reset(true);
                    fractalComponent.startNewFractal(false);
                    addFormula(getExpressionText());
                    break;
                }
                    
                case ANIM_CONSTANT_START: {
                    JTextField field = (JTextField)ac.getSource();
                    //AFractal frac= fractalComponent.getFractal();
                    evaluator.parse(field.getText());
                    fractalComponent.setConstant(evaluator.evaluate());
                    setProperty(PROP_ANIM_START_FIELD, field.getText());
                    fractalComponent.reset(false);
                    fractalComponent.startNewFractal(false);
                    break;
                }
                case ANIM_CONSTANT_END: {
                    JTextField field = (JTextField)ac.getSource();
                    evaluator.parse(field.getText());
                    fractalComponent.setConstant(evaluator.evaluate());
                    setProperty(PROP_ANIM_END_FIELD, field.getText());
                    fractalComponent.reset(false);
                    fractalComponent.startNewFractal(false);
                    break;
                }
                case CUSTOM_SET_CONSTANT: {
                    JTextField field = (JTextField)ac.getSource();
                    evaluator.parse(field.getText());
                    fractalComponent.setConstant(evaluator.evaluate());
                    setProperty(PROP_CONSTANT_EXPRESSION, field.getText());
                    fractalComponent.reset(false);
                    fractalComponent.startNewFractal(false);
                    break;
                }
                
                case INVERT_COLORS: {
                    fractalComponent.getColorTable().invertColors();
                    fractalComponent.startNewFractal(true);
                    break;
                }
                
                case BRIGHTEN_COLORS:{
                    fractalComponent.getColorTable().brightenColors();
                    fractalComponent.startNewFractal(true);
                    break;
                }
        
                case DARKEN_COLORS:{
                    fractalComponent.getColorTable().darkenColors();
                    fractalComponent.startNewFractal(true);
                    break;
                }
                
                case ROTATE_COLORS: {
                    break;
                }
                
                case CANCEL:
                    animation.stop();
                    break;
                    
                case SHOW_WATERMARK: {
                	boolean selected = ((AbstractButton)ac.getSource()).isSelected();
                    fractalComponent.setShowWatermark(selected);
                    setProperty(PROP_SHOW_WATERMARK_BOOLEAN, String.valueOf(selected));
                    break;
                }

                case MAKE_MOVIE: {
                    makeMovie();
                }
            }
        } catch (TokenMgrError e) {
            showError(e.getClass().getSimpleName() +  ":" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError(e.getClass().getSimpleName() +  ":" + e.getMessage());
        }
    }
    
    void loadFormulas() {
        BufferedReader in = null;
        try {
            File file = FORMULAS_FILE;
            if (file.exists()) {
                System.out.println("Loading file " + file.getAbsolutePath());
                in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                while (true) {
                    String formula = in.readLine();
                    if (formula == null)
                        break;
                    if (!formulas.contains(formula))
                        formulas.add(formula);
                }
                System.out.println("Loaded " + formulas.size() + " formulas");
            } else {
                formulas.add("Z^2 + Z0");
                formulas.add("Z^2 - C");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) {}
            }
        }
    }
    
    void addFormula(String formula) {
        formulas.remove(formula);
        formulas.add(0, formula);
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(FORMULAS_FILE));
            for (String f : formulas) {
                out.println(f);
            }
        } catch (Exception e) {
            this.showError(e);
            e.printStackTrace();
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception e) {}
            }
        }
    }
    
    @Override
    public void onWindowClosing() {
        animation.stop();
    }

}

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Client-server graphical editor
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012; loosely based on CS 5 code by Tom Cormen
 * @author CBK, winter 2014, overall structure substantially revised
 * @author Travis Peters, Dartmouth CS 10, Winter 2015; remove EditorCommunicatorStandalone (use echo server for testing)
 * @author CBK, spring 2016 and Fall 2016, restructured Shape and some of the GUI
 * @author Avi Dixit & Hannah Brookes, CS10, March 2020
 */

public class Editor extends JFrame {
    private static String serverIP = "localhost";            // IP address of sketch server
    // "localhost" for your own machine;
    // or ask a friend for their IP address

    private static final int width = 800, height = 800;        // canvas size

    // Current settings on GUI
    public enum Mode {
        DRAW, MOVE, RECOLOR, DELETE
    }

    private Mode mode = Mode.DRAW;                // drawing/moving/recoloring/deleting objects
    private String shapeType = "ellipse";        // type of object to add
    private Color color = Color.black;            // current drawing color

    // Drawing state
    // these are remnants of my implementation; take them as possible suggestions or ignore them
    private Shape curr = null;                    // current shape (if any) being drawn
    private Sketch sketch;                        // holds and handles all the completed objects
    private Point drawFrom = null;                // where the drawing started
    private Point moveFrom = null;                // where object is as it's being dragged


    // Communication
    private EditorCommunicator comm;            // communication with the sketch server

    public Editor() {
        super("Graphical Editor");

        sketch = new Sketch();

        // Connect to server
        comm = new EditorCommunicator(serverIP, this);
        comm.start();

        // Helpers to create the canvas and GUI (buttons, etc.)
        JComponent canvas = setupCanvas();
        JComponent gui = setupGUI();

        // Put the buttons and canvas together into the window
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(canvas, BorderLayout.CENTER);
        cp.add(gui, BorderLayout.NORTH);

        // Usual initialization
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    /**
     * Creates a component to draw into
     */
    private JComponent setupCanvas() {
        JComponent canvas = new JComponent() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawSketch(g);
            }
        };

        canvas.setPreferredSize(new Dimension(width, height));

        canvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                handlePress(event.getPoint());
            }

            public void mouseReleased(MouseEvent event) {
                handleRelease();
            }
        });

        canvas.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent event) {
                handleDrag(event.getPoint());
            }
        });

        return canvas;
    }

    /**
     * Creates a panel with all the buttons
     */
    private JComponent setupGUI() {
        // Select type of shape
        String[] shapes = {"ellipse", "rectangle", "segment"};
        JComboBox<String> shapeB = new JComboBox<String>(shapes);
        shapeB.addActionListener(e -> shapeType = (String) ((JComboBox<String>) e.getSource()).getSelectedItem());

        // Select drawing/recoloring color
        // Following Oracle example
        JButton chooseColorB = new JButton("choose color");
        JColorChooser colorChooser = new JColorChooser();
        JLabel colorL = new JLabel();
        colorL.setBackground(Color.black);
        colorL.setOpaque(true);
        colorL.setBorder(BorderFactory.createLineBorder(Color.black));
        colorL.setPreferredSize(new Dimension(25, 25));
        JDialog colorDialog = JColorChooser.createDialog(chooseColorB,
                "Pick a Color",
                true,  //modal
                colorChooser,
                e -> {
                    color = colorChooser.getColor();
                    colorL.setBackground(color);
                },  // OK button
                null); // no CANCEL button handler
        chooseColorB.addActionListener(e -> colorDialog.setVisible(true));

        // Mode: draw, move, recolor, or delete
        JRadioButton drawB = new JRadioButton("draw");
        drawB.addActionListener(e -> mode = Mode.DRAW);
        drawB.setSelected(true);
        JRadioButton moveB = new JRadioButton("move");
        moveB.addActionListener(e -> mode = Mode.MOVE);
        JRadioButton recolorB = new JRadioButton("recolor");
        recolorB.addActionListener(e -> mode = Mode.RECOLOR);
        JRadioButton deleteB = new JRadioButton("delete");
        deleteB.addActionListener(e -> mode = Mode.DELETE);
        ButtonGroup modes = new ButtonGroup(); // make them act as radios -- only one selected
        modes.add(drawB);
        modes.add(moveB);
        modes.add(recolorB);
        modes.add(deleteB);
        JPanel modesP = new JPanel(new GridLayout(1, 0)); // group them on the GUI
        modesP.add(drawB);
        modesP.add(moveB);
        modesP.add(recolorB);
        modesP.add(deleteB);

        // Put all the stuff into a panel
        JComponent gui = new JPanel();
        gui.setLayout(new FlowLayout());
        gui.add(shapeB);
        gui.add(chooseColorB);
        gui.add(colorL);
        gui.add(modesP);
        return gui;
    }

    /**
     * Getter for the sketch instance variable
     */
    public Sketch getSketch() {
        return sketch;
    }

    /**
     * Draws all the shapes in the sketch,
     * along with the object currently being drawn in this editor (not yet part of the sketch)
     */
    public void drawSketch(Graphics g) {
        // gets a list of all the shapes in the sketch and draws each shape on the screen
        List<Shape> shapes = sketch.getShapes();
        for (Shape s : shapes) {
            s.draw(g);
            repaint();
        }
        // draws the current shape being manipulated in real time locally
        if (curr != null) {
            curr.draw(g);
            repaint();
        }
    }

    // Helpers for event handlers

    /**
     * Helper method for press at point
     * In drawing mode, start a new object;
     * in moving mode, (request to) start dragging if clicked in a shape;
     * in recoloring mode, (request to) change clicked shape's color
     * in deleting mode, (request to) delete clicked shape
     */
    private void handlePress(Point p) {
        // In drawing mode, start drawing a new shape based on the current shape type
        if (mode == Mode.DRAW) {
            // sets the drawFrom point to be the point at which the mouse was pressed
            drawFrom = p;
            // draws an ellipse
            if (shapeType.equals("ellipse")) {
                curr = new Ellipse((int) drawFrom.getX(), (int) drawFrom.getY(), color);
            }
            // draws a rectangle
            else if (shapeType.equals("rectangle")) {
                curr = new Rectangle((int) drawFrom.getX(), (int) drawFrom.getY(), color);
            }
            // draws a segment
            else if (shapeType.equals("segment")) {
                curr = new Segment((int) drawFrom.getX(), (int) drawFrom.getY(), color);
                // sets the start coordinates of the segment
                ((Segment) curr).setStart((int) drawFrom.getX(), (int) drawFrom.getY());
            }

        }

        // In moving mode, send a message to start dragging
        if (mode == Mode.MOVE) {
            comm.send("move press " + (int) p.getX() + " " + (int) p.getY());
            // sets the moveFrom point to the point where the mouse was pressed
            moveFrom = p;
        }
        // In recoloring mode, send a message to change the shape's color, repaint the screen
        else if (mode == Mode.RECOLOR) {
            comm.send("recolor " + (int) p.getX() + " " + (int) p.getY() + " " + color.getRGB());
            repaint();
        }
        // In deleting mode, send a message to delete the shape if, then repaint the screen
        else if (mode == Mode.DELETE) {
            comm.send("delete " + (int) p.getX() + " " + (int) p.getY());
            repaint();
        }

    }

    /**
     * Helper method for drag to new point
     * In drawing mode, update the other corner of the object;
     * in moving mode, (request to) drag the object
     */
    private void handleDrag(Point p) {
        // In drawing mode, revise the shape as it is stretched out
        if (mode == Mode.DRAW) {
            // if the shape is an ellipse that is being drawn, reset its corners updating x2 and y2 based on
            // the point where the mouse is dragged to and repaint
            if (shapeType.equals("ellipse")) {
                ((Ellipse) curr).setCorners((int) drawFrom.getX(), (int) drawFrom.getY(), (int) p.getX(), (int) p.getY());
                repaint();
            }
            // if the shape is a rectangle that is being drawn, reset its corners updating x2 and y2 based on
            // the point where the mouse is dragged to and repaint
            else if (shapeType.equals("rectangle")) {
                ((Rectangle) curr).setCorners((int) drawFrom.getX(), (int) drawFrom.getY(), (int) p.getX(), (int) p.getY());
                repaint();
            }
            // if the shape is a segment that is being drawn, set its end points to the point where the mouse
            // is dragged to and repaint
            else if (shapeType.equals("segment")) {
                ((Segment) curr).setEnd((int) p.getX(), (int) p.getY());
                repaint();
            }
        }

        if (mode == Mode.MOVE) {

            // if the shape exists and there is a point to move from, subtract the coordinates of moveFrom from
            // the current point's coordinates and send a message to move the shape by the difference between the two
            // update the moveFrom point to the current point of the mouse
            // repaint
            if (moveFrom != null) {
                double xMoved = p.getX() - moveFrom.getX();
                double yMoved = p.getY() - moveFrom.getY();
                comm.send("move drag "+(int)xMoved + " " + (int)yMoved);
                moveFrom = p;
            }
        }

    }

    /**
     * Helper method for release
     * In drawing mode, pass the add new object request on to the server;
     * in moving mode, release it
     */
    private void handleRelease() {
        // if drawing mode, send a message to draw the current shape and then set current to null
        if (mode == Mode.DRAW) {
            comm.send("draw " +curr.toString());
            curr = null;
        }
        // In moving mode, stop dragging the object
        if (mode == Mode.MOVE) moveFrom = null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Editor();
            }
        });
    }
}
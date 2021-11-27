import java.util.*;

/**
 * Class to handle a sketch -> keeps a map of all the shapes to be drawn
 * @author Avi Dixit & Hannah Brookes, CS10, March 2020
 */

public class Sketch {

    // map to keep track of shape id and shape
    private TreeMap<Integer, Shape> shapeMap;

    // initializes a new empty map to hold shape ids and shapes
    public Sketch() {
        shapeMap = new TreeMap<Integer, Shape>();
    }

    // returns a list of shapes with keys sorted in ascending order such that the shapes first in the list
    // are the shapes to be drawn first
    public List<Shape> getShapes() {
        List<Shape> shapes = new ArrayList<Shape>();
        for (Integer shapeID : shapeMap.navigableKeySet()) {
            shapes.add(shapeMap.get(shapeID));
        }
        return shapes;
    }

    // adds a shape to the map given an id and a shape
    public void addShape(int id, Shape shape) {
        shapeMap.put(id, shape);
    }

    // returns the shape map
    public Map<Integer, Shape> getShapeMap() {
        return shapeMap;
    }

    // removes a shape given an x and y coordinate
    public void removeShape(int x, int y) {
        // loops through the id keys of the shape map in descending order so that the first shape to match
        // the coordinates is the newest one added ("on top" in the drawing)
        for (Integer shapeID : shapeMap.descendingKeySet()) {
            if (shapeMap.get(shapeID).contains(x,y)) {
                shapeMap.remove(shapeID);
                break;
            }
        }
    }

    // removes a shape given an id
    public void removeShapeByID(int id) {
        shapeMap.remove(id);
    }

    // returns a shape given an id
    public Shape getShapeFromID(int id) {
        return shapeMap.get(id);
    }

    // gets the id of a shape based on an x and y coordinate
    public int getId(int x, int y) {
        // loops through the id keys of the shape map in descending order so that the first shape to match
        // the coordinates is the newest one added ("on top" in the drawing)
        for (Integer shapeID : shapeMap.descendingKeySet()) {
            if (shapeMap.get(shapeID).contains(x,y)) return shapeID;
        }
        return -1;
    }

}

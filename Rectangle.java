import java.awt.Color;
import java.awt.Graphics;

/**
 * A rectangle-shaped Shape
 * Defined by an upper-left corner (x1,y1) and a lower-right corner (x2,y2)
 * with x1<=x2 and y1<=y2
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012
 * @author CBK, updated Fall 2016
 * @author Avi Dixit & Hannah Brookes, CS10, March 2020
 *
 */
public class Rectangle implements Shape {
	private int x1,y1,x2,y2;
	private Color color;

	// constructor for a rectangle given an x and y coordinate and a color
	public Rectangle(int x, int y, Color color) {
		// set x2=x1 and y2=y1
		this.x1 = x; this.x2 = x;
		this.y1 = y; this.y2 = y;
		this.color = color;
	}

	// constructor given two sets of x and y coordinates and a color -> calls the setCorners method to
	// set update the rectangles x1, y1, x2, and y2 values
	public Rectangle(int x1, int y1, int x2, int y2, Color color) {
		setCorners(x1,y1,x2,y2);
		this.color = color;
	}

	// sets the corners of the rectangle based on x1, y1, x2, and y2
	public void setCorners(int x1, int y1, int x2, int y2) {
		// Ensure correct upper left and lower right
		this.x1 = Math.min(x1, x2);
		this.y1 = Math.min(y1, y2);
		this.x2 = Math.max(x1, x2);
		this.y2 = Math.max(y1, y2);
	}

	@Override
	// moves the rectangle by a given dx and dy -> adds dx to each x coordinate and dy to each y coordinate
	public void moveBy(int dx, int dy) {
		x1 += dx; x2 += dx;
		y1 += dy; y2 += dy;
	}

	// returns the color of the rectangle
	@Override
	public Color getColor() {
		return color;
	}

	// sets the color of the rectangle
	@Override
	public void setColor(Color color) {
		this.color = color;
	}

	// returns true if the rectangle contains the point at (x, y)
	@Override
	public boolean contains(int x, int y) {
		return x1 <= x && x2 >= x && y1 <= y && y2 >= y;
	}

	// draws the rectangle
	@Override
	public void draw(Graphics g) {
		g.setColor(color);
		g.fillRect(x1, y1, x2-x1, y2-y1);
	}

	// returns a string representation of a rectangle including its upper left and lower right corner coordinates
	// and its color
	public String toString() {
		return "rectangle "+x1+" "+y1+" "+x2+" "+y2+" "+color.getRGB();
	}
}
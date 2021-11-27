import java.awt.*;
import java.io.*;
import java.net.Socket;;

/**
 * Handles communication to/from the server for the editor
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012
 * @author Chris Bailey-Kellogg; overall structure substantially revised Winter 2014
 * @author Travis Peters, Dartmouth CS 10, Winter 2015; remove EditorCommunicatorStandalone (use echo server for testing)
 * @author Avi Dixit & Hannah Brookes, CS10, March 2020
 */
public class EditorCommunicator extends Thread {
	private PrintWriter out;      // to server
	private BufferedReader in;    // from server
	protected Editor editor;      // handling communication for
	private Shape curr = null;	  // current shape being handled

	/**
	 * Establishes connection and in/out pair
	 */
	public EditorCommunicator(String serverIP, Editor editor) {
		this.editor = editor;
		System.out.println("connecting to " + serverIP + "...");
		try {
			Socket sock = new Socket(serverIP, 4242);
			out = new PrintWriter(sock.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			System.out.println("...connected");
		}
		catch (IOException e) {
			System.err.println("couldn't connect");
			System.exit(-1);
		}
	}

	/**
	 * Sends message to the server
	 */
	public void send(String msg) {
		out.println(msg);
	}

	/**
	 * Keeps listening for and handling (your code) messages from the server
	 */
	public void run() {
		try {
			// Handle messages
			String line;

			// while there are messaged coming in, continue
			while ((line = in.readLine()) != null) {
				// Array of the strings in message split by spaces
				String[] split = line.strip().split(" ");
				// The first string in the array is the 'command' to do
				String command = split[0];
				// Keep track of the id of the current shape
				int currId = -1;

				// if command is join, then update the new editor on the state of the server sketch
				if (command.equals("join")) {
					// set currId to be the second value received from the server
					currId = Integer.parseInt(split[1]);
					// if the shape is a rectangle, then make a new rectangle with id currID
					if (split[2].equals("rectangle")) {
						editor.getSketch().addShape(currId, new Rectangle(
								Integer.parseInt(split[3]),Integer.parseInt(split[4]), Integer.parseInt(split[5]),
								Integer.parseInt(split[6]), new Color(Integer.parseInt(split[7]))));
					}
					// else if the shape is an ellipse, then make a new ellipse with id currID
					else if (split[2].equals("ellipse")) {
						editor.getSketch().addShape(currId, new Ellipse(
								Integer.parseInt(split[3]),Integer.parseInt(split[4]), Integer.parseInt(split[5]),
								Integer.parseInt(split[6]), new Color(Integer.parseInt(split[7]))));
					}
					// else if the shape is a segment, then make a new segment with id currID
					else if (split[2].equals("segment")) {
						editor.getSketch().addShape(currId, new Segment(
								Integer.parseInt(split[3]),Integer.parseInt(split[4]), Integer.parseInt(split[5]),
								Integer.parseInt(split[6]), new Color(Integer.parseInt(split[7]))));
					}

				}
				// else if the command is draw, then draw a new shape on the editor's screen
				else if (command.equals("draw")) {
					// set currId to be the second value received from the server
					currId = Integer.parseInt(split[1]);
					// if the shape is a rectangle, then set curr to be a new rectangle with id currID
					if (split[2].equals("rectangle")) {
						 curr = new Rectangle(
								Integer.parseInt(split[3]),Integer.parseInt(split[4]), Integer.parseInt(split[5]),
								Integer.parseInt(split[6]), new Color(Integer.parseInt(split[7])));
					}
					// else if the shape is an ellipse, then set curr to a new ellipse with id currID
					else if (split[2].equals("ellipse")) {
						curr = new Ellipse(
								Integer.parseInt(split[3]),Integer.parseInt(split[4]), Integer.parseInt(split[5]),
								Integer.parseInt(split[6]), new Color(Integer.parseInt(split[7])));
					}
					// else if the shape is a segment, then set curr to a new segment with id currID
					else if (split[2].equals("segment")) {
						curr = new Segment(
								Integer.parseInt(split[3]),Integer.parseInt(split[4]), Integer.parseInt(split[5]),
								Integer.parseInt(split[6]), new Color(Integer.parseInt(split[7])));
					}
					// add the new curr shape to the editor's sketch and repaint to update
					editor.getSketch().addShape(currId, curr);
					editor.repaint();
				}
				// else if the command is move, then move the current object
				else if (command.equals("move")) {
					// secondCommand is whether the user is pressing or dragging right now
					String secondCommand = split[1];

					// if the user is moving and dragging, then move the shape connected to the id passed by the server
					// the dx and dy also passed by the server
					if (secondCommand.equals("drag")) {
						int currDragId = Integer.parseInt(split[2]);
						int dx = Integer.parseInt(split[3]);
						int dy = Integer.parseInt(split[4]);
						curr = editor.getSketch().getShapeFromID(currDragId);
						// if current exists, then move the current shape and repaint
						if (curr != null) {
							curr.moveBy(dx, dy);
							editor.repaint();
						}
					}
				}
				// else if the command is recolor, the recolor the current object
				else if (command.equals("recolor")) {
					// current is the shape associated with the id passed by the server
					currId = Integer.parseInt(split[1]);
					curr = editor.getSketch().getShapeFromID(currId);
					// if current exists, change the color of the shape and repaint
					if (curr != null) {
						curr.setColor(new Color(Integer.parseInt(split[2])));
						editor.repaint();
					}

				}
				// else if the command is delete, delete the current shape
				else if (command.equals("delete")) {
					// current is the shape associated with the id passed by the server
					currId = Integer.parseInt(split[1]);
					curr = editor.getSketch().getShapeFromID(currId);
					// if current exists, delete the shape and repaint
					if (curr != null) {
						editor.getSketch().removeShapeByID(currId);
						editor.repaint();
					}

				}

			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			System.out.println("server hung up");
		}
	}
}

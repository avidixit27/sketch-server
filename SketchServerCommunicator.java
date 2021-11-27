import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Handles communication between the server and one client, for SketchServer
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012; revised Winter 2014 to separate SketchServerCommunicator
 * @author Avi Dixit & Hannah Brookes, CS10, March 2020
 */
public class SketchServerCommunicator extends Thread {
    private Socket sock;                    // to talk with client
    private BufferedReader in;                // from client
    private PrintWriter out;                // to client
    private SketchServer server;            // handling communication for
    private static int id = 0;				// keeps track of the id for a newly added shape
    private Shape curr = null;				// keeps track of the current shape (the one being moved, etc.)
    private int currId = -1;				// the id of the current shape (-1 unless otherwise stated)

    public SketchServerCommunicator(Socket sock, SketchServer server) {
        this.sock = sock;
        this.server = server;
    }

    /**
     * Sends a message to the client
     *
     * @param msg
     */
    public void send(String msg) {
        out.println(msg);
    }

    /**
     * Keeps listening for and handling (your code) messages from the client
     */
    public void run() {
        try {
            System.out.println("someone connected");

            // Communication channel
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintWriter(sock.getOutputStream(), true);

            // Tell the client the current state of the world
            Map<Integer, Shape> map = server.getSketch().getShapeMap();
            // for each id in the map key set, broadcast a message to the client including the id of the shape
            // and the shape to be added
            for (int id : map.keySet()) {
                server.broadcast("join " + id + " " + map.get(id).toString());
            }

            // Keep getting and handling messages from the client
            String line;
            while ((line = in.readLine()) != null) {
                // splits the lines based on spaces
                String[] split = line.strip().split(" ");
                // determines which command was sent in the message
                String command = split[0];

                // if the command is draw, add a shape to the shape map based on the shape type passed in
                // broadcast the message back to all communicators to add the new shape
                if (command.equals("draw")) {
                    // sets current to a new rectangle based on the coordinates passed in in the message
                    if (split[1].equals("rectangle")) {
                        curr = new Rectangle(
                                Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]),
                                Integer.parseInt(split[5]), new Color(Integer.parseInt(split[6])));
                    }
                    // sets current to a new ellipse based on the coordinates passed in in the message
                    else if (split[1].equals("ellipse")) {
                        curr = new Ellipse(
                                Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]),
                                Integer.parseInt(split[5]), new Color(Integer.parseInt(split[6])));
                    }
                    // sets current to a new segment based on the coordinates passed in in the message
                    else if (split[1].equals("segment")) {
                        curr = new Segment(
                                Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]),
                                Integer.parseInt(split[5]), new Color(Integer.parseInt(split[6])));
                    }
                    // adds the shape to the master sketch
                    server.getSketch().addShape(id, curr);
                    // broadcasts the message
                    server.broadcast("draw " + id + " " + curr.toString());
                    // increments the static variable id to ensure next shape added has an id incremented by one
                    id++;

                }
                // if the command is move, determine whether the secondary command is press or drag
                else if (command.equals("move")) {
                    String secondCommand = split[1];

                    // if the secondary command is press, update the currId and curr shape based on the
                    // coordinates passed in the message
                    if (secondCommand.equals("press")) {
                        currId = server.getSketch().getId(Integer.parseInt(split[2]), Integer.parseInt(split[3]));
                        curr = server.getSketch().getShapeFromID(currId);
                        // if there is a shape at the given coordinates, make it so that the shape will move to the
                        // front of other shapes
                        if (curr != null && curr.contains(Integer.parseInt(split[2]), Integer.parseInt(split[3]))) {
                            //add a new shape with a new id and the curr shape information to sketch's shape map,
                            // broadcast the message to do the same
                            server.getSketch().addShape(id, curr);
                            server.broadcast("draw "+ id+" "+curr.toString());
                            // delete the original shape based on its id (so you don't have duplicate shape) and
                            // broadcast the message to do the same
                            server.getSketch().removeShapeByID(currId);
                            server.broadcast("delete " + currId);
                            // update currId to the new id
                            currId = id;
                            // update curr to the new currId
                            curr = server.getSketch().getShapeFromID(currId);
                            // increment id by one
                            id++;
                        }
                    }
                    // if the secondary command is drag, move the curr shape based on the dx and dy parameters
                    // from the message
                    else if (secondCommand.equals("drag")) {
                        int dx, dy;
                        dx = Integer.parseInt(split[2]);
                        dy = Integer.parseInt(split[3]);
                        // if  curr exists and its id is greater than or equal to 0, move the shape based on
                        // dx and dy and broadcast the message to do the same
                        if (curr != null && currId >= 0) {
                            server.getSketch().getShapeFromID(currId).moveBy(dx, dy);
                            server.broadcast("move drag " + currId + " " + dx + " " + dy);
                        }
                    }
                }
                // if the command is recolor, determine the curr shape and currId based on the x and y parameters that
                // were passed in
                else if (command.equals("recolor")) {
                    currId = server.getSketch().getId(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                    curr = server.getSketch().getShapeFromID(currId);
                    // if curr exists, update the curr shape's color and broadcast the message to do the same
                    if (curr != null) {
                        curr.setColor(new Color(Integer.parseInt(split[3])));
                        server.broadcast("recolor " + currId + " " + split[3]);
                    }
                }
                // if the command is delete, determine the curr shape and currId based on the x and y parameters
                // that were passed in
                else if (command.equals("delete")) {
                    int x, y;
                    x = Integer.parseInt(split[1]);
                    y = Integer.parseInt(split[2]);
                    currId = server.getSketch().getId(x, y);
                    curr = server.getSketch().getShapeFromID(currId);
                    // if the curr shape exists and it does contain x and y, remove the shape from sketch's shapeMap
                    // and broadcast the message to do the same
                    if (curr != null && curr.contains(x, y)) {
                        server.getSketch().removeShapeByID(currId);
                        server.broadcast("delete " + currId);
                    }
                }
            }

            // Clean up -- note that also remove self from server's list so it doesn't broadcast here
            server.removeCommunicator(this);
            out.close();
            in.close();
            sock.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("error in sketch server communicator");
        }
    }
}
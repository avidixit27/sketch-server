# sketch-server
## Avi Dixit 
### February 2020

### Description
Java client and server implementation to allow multiple users to draw and edit the same canvas in real time. 

### Implementation
  * The Sketch Server controls the server side. It provides users with a port to join, and validates clients as they join. 
  * The Sketch Server communicator parses messages being sent from the client to the server and calls the proper method on sketch server
  * The editor controls the client side. It includes methods to draw, add points, adjust points, and delete shapes. 
  * The user can draw segements, circles, rectangles, and free hand shapes.
  * The editor communicator creates messages and sends them to the server to be parsed

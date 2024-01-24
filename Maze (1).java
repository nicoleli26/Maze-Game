import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.LineImage;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;
import tester.Tester;
// INSTRUCTIONS:
// Search: If you would like to see the search, press "b" to see breadth-first search,
//  and press "d" to see depth-first search.
// Solution: If you would like to see the solution, press "enter"
// Edge bias:
//  If you would like the maze to be horizontally biased, press "h"
//  If you would like the maze to be vertically biased, press "v"
// Reset: If you would like to reset with a new random maze, press "r"
// Traversing: Use up, left, right, and down arrows to traverse the maze.
//  If the end is reached, the user has completed the maze
// Clear: If you want to clear the maze without making a new random one,
//  press backspace

// comparator to compare edge weights
class CompareEdgeWeights implements Comparator<Edge> {
  public int compare(Edge e1, Edge e2) {
    return Integer.compare(e1.weight, e2.weight);
  }
}

// Cell represents each cell in the grid and each pair of cells has a Wall dividing them
class Cell {
  static int SCALE = 10;
  int x;
  int y;
  boolean top;
  boolean right;
  boolean left;
  boolean bottom;
  ArrayList<Edge> outEdges; // edges from this node
  Color color;

  Cell(int x, int y) {
    this.x = x;
    this.y = y;
    this.outEdges = new ArrayList<Edge>();
    this.top = true;
    this.right = true;
    this.left = true;
    this.bottom = true;
    this.color = Color.white;
  }

  // draws an image of a cell with lines
  WorldImage drawCell(Color color) {
    return new RectangleImage(SCALE, SCALE, "solid", color);
  }

  // gets the x-coordinate for the cell image
  int cellX() {
    return SCALE / 2 + (this.x * SCALE);
  }

  // gets the y-coordinate for the cell image
  int cellY() {
    return SCALE / 2 + (this.y * SCALE);
  }

  // changes this cell's color to blue
  void changeColor(Color givenColor) {
    this.color = givenColor;
  }
}

// Edge represents a connection between two cells; if an edge is made, a wall must be removed
// given a random weight (worry about getting the image later)
class Edge {
  Cell from;
  Cell to;
  int weight;

  Edge(Cell from, Cell to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  // determines if this edge is horizontal
  boolean isHorizontal() {
    return this.from.y == this.to.y;
  }
}

// represents a random maze
class MazeWorld extends World {
  // if maze is smaller than 50 x 50, scale is 20
  static int SCALE = 10;
  int length;
  int height;
  Random rand;
  ArrayList<ArrayList<Cell>> board;
  ArrayList<Edge> edges;
  ArrayList<Edge> mst;
  ArrayList<Cell> visitedBFS;
  ArrayList<Cell> visitedDFS;
  // true if b is pressed
  boolean breadthFirst;
  // true if d is pressed
  boolean depthFirst;
  // true if breadth or depth first are running
  boolean searching;
  // true if search is done
  boolean end;
  // keeps track of ticks
  int tick;
  HashMap<Cell, Cell> cameFromCells;
  // true if done back tracking
  boolean doneBackTracking;
  // current traversing cell's x value
  int curX;
  // current traversing cell's y value
  int curY;
  // true if h is pressed
  boolean preferHorizontal;
  // true if v is pressed
  boolean preferVertical;
  // true if BFS is done
  boolean doneBFS;
  // true if DFS is done
  boolean doneDFS;
  // true if user finishes maze
  boolean completed;

  MazeWorld(int length, int height) {
    this(length, height, new Random());
  }

  // seeded random for testing
  MazeWorld(int length, int height, Random rand) {
    this.length = length;
    this.height = height;
    this.rand = rand;
    this.board = new ArrayList<ArrayList<Cell>>();
    for (int row = 0; row < height; row = row + 1) {
      board.add(new ArrayList<Cell>());
      for (int column = 0; column < length; column = column + 1) {
        board.get(row).add(new Cell(column, row));
      }
    }
    this.edges = new ArrayList<Edge>();
    this.addEdges();
    this.mst = this.kruskal();
    // if false, don't draw edge
    for (Edge e : this.mst) {
      if (e.isHorizontal()) {
        e.from.right = false;
        e.to.left = false;
      }
      else {
        e.from.bottom = false;
        e.to.top = false;
      }
    }
    this.visitedBFS = new ArrayList<Cell>();
    this.visitedDFS = new ArrayList<Cell>();
    this.breadthFirst = false;
    this.depthFirst = false;
    this.searching = false;
    this.end = false;
    this.tick = 0;
    this.cameFromCells = new HashMap<Cell, Cell>();
    this.doneBackTracking = false;
    this.curX = 0;
    this.curY = 0;
    this.preferHorizontal = false;
    this.preferVertical = false;
    this.doneBFS = false;
    this.doneDFS = false;
    this.completed = false;
  }

  // adds edges to this array list of edges
  public void addEdges() {
    for (int row = 0; row < height; row = row + 1) {
      for (int col = 0; col < length; col = col + 1) {
        int edgeWeight = this.rand.nextInt(100000);
        if (col < length - 1) {
          if (this.preferHorizontal) {
            Edge edge = new Edge(board.get(row).get(col), board.get(row).get(col + 1),
                edgeWeight - 100000);
            edges.add(edge);
          }
          else {
            Edge edge = new Edge(board.get(row).get(col), board.get(row).get(col + 1), edgeWeight);
            edges.add(edge);
          }
        }
        if (row < height - 1) {
          if (this.preferVertical) {
            Edge edge = new Edge(board.get(row).get(col), board.get(row + 1).get(col),
                edgeWeight - 100000);
            edges.add(edge);
          }
          else {
            Edge edge = new Edge(board.get(row).get(col), board.get(row + 1).get(col), edgeWeight);
            edges.add(edge);
          }
        }
      }
    }
  }

  // renders the board
  public WorldScene makeScene() {
    WorldScene background = new WorldScene(length * SCALE, height * SCALE);
    if (this.completed) {
      return lastScene("Maze is complete!");
    }
    for (ArrayList<Cell> row : board) {
      for (Cell cell : row) {
        if (cell.equals(this.board.get(0).get(0))) {
          cell.changeColor(Color.green);
        }
        if (cell.equals(this.board.get(height - 1).get(length - 1))) {
          cell.changeColor(Color.magenta);
        }
        background.placeImageXY(cell.drawCell(cell.color), cell.cellX(), cell.cellY());
        if (cell.top) {
          background.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), cell.cellX(),
              cell.cellY() - SCALE / 2);
        }
        if (cell.left) {
          background.placeImageXY(new LineImage(new Posn(0, SCALE), Color.black),
              cell.cellX() - SCALE / 2, cell.cellY());
        }
      }
    }
    return background;
  }

  public WorldScene lastScene(String msg) {
    WorldScene endImage = new WorldScene(1000, 700);
    endImage.placeImageXY(new TextImage(msg, 20, Color.black), 500, 350);
    endImage.placeImageXY(new TextImage("Click 'r' to reset", 15, Color.red), 500, 400);
    endImage.placeImageXY(new TextImage("Click 'backspace' to clear", 15, Color.black), 500, 430);
    return endImage;
  }

  // gets minimum spanning tree
  // and adds edges to cell's outEdges
  public ArrayList<Edge> kruskal() {
    // If two vertices have the same integer, they are connected
    // check if any other cell's value is connected to the stored local value and
    // change it to the new value as well
    HashMap<Cell, Integer> representatives = new HashMap<Cell, Integer>();
    ArrayList<Edge> mst = new ArrayList<Edge>();
    Collections.sort(this.edges, new CompareEdgeWeights());
    int val = 0;
    for (int row = 0; row < height; row = row + 1) {
      for (int col = 0; col < length; col = col + 1) {
        representatives.put(this.board.get(row).get(col), val);
        val = val + 1;
      }
    }
    int cellsVisitedCount = 0;
    while (cellsVisitedCount < height * length - 1) {
      for (Edge e : this.edges) {
        if (representatives.get(e.from) == representatives.get(e.to)) {
          // do nothing
        }
        else {
          // store old value
          int oldValue = representatives.get(e.to);
          mst.add(e);
          e.from.outEdges.add(e);
          e.to.outEdges.add(e);
          representatives.put(e.to, representatives.get(e.from));
          cellsVisitedCount = cellsVisitedCount + 1;
          for (Cell cell : representatives.keySet()) {
            if (representatives.get(cell) == oldValue) {
              representatives.put(cell, representatives.get(e.from));
            }
          }
        }
      }
    }
    return mst;
  }

  // searches cells using breadth-first search
  public boolean breadthFirst() {
    HashMap<Cell, Cell> cameFromCell = new HashMap<Cell, Cell>();
    LinkedList<Cell> worklist = new LinkedList<Cell>();
    worklist.add(this.board.get(0).get(0));
    while (!worklist.isEmpty()) {
      Cell next = worklist.remove();
      visitedBFS.add(next);
      if (next.equals(this.board.get(height - 1).get(length - 1))) {
        this.cameFromCells = cameFromCell;
        this.doneBFS = true;
        return true;
      }
      for (Edge e : next.outEdges) {
        if ((e.from.equals(next)) && !(visitedBFS.contains(e.to))) {
          worklist.add(e.to);
          cameFromCell.put(e.to, next);
        }
        else if ((e.to.equals(next)) && !(visitedBFS.contains(e.from))) {
          worklist.add(e.from);
          cameFromCell.put(e.from, next);
        }
      }
    }
    return false;
  }

  // searches cells using depth-first search
  public boolean depthFirst() {
    HashMap<Cell, Cell> cameFromCell = new HashMap<Cell, Cell>();
    Stack<Cell> worklist = new Stack<Cell>();
    worklist.add(this.board.get(0).get(0));
    while (!worklist.isEmpty()) {
      Cell next = worklist.pop();
      visitedDFS.add(next);
      if (next.equals(this.board.get(height - 1).get(length - 1))) {
        this.cameFromCells = cameFromCell;
        this.doneDFS = true;
        return true;
      }
      for (Edge e : next.outEdges) {
        if ((e.from.equals(next)) && !(visitedDFS.contains(e.to))) {
          worklist.push(e.to);
          cameFromCell.put(e.to, next);
        }
        else if ((e.to.equals(next)) && !(visitedDFS.contains(e.from))) {
          worklist.push(e.from);
          cameFromCell.put(e.from, next);
        }
      }
    }
    return false;
  }

  // if the b key is pressed, run breadth-first search
  // if the d key is pressed, run depth-first search
  // if the r key is pressed, reset with new random maze
  // if the h key is pressed, reset with a horizontally-biased maze
  // if the v key is pressed, reset with a vertically-biased maze
  // if the backspace key is pressed, clear the maze
  // if the enter key is pressed, reset the board and show the correct path
  // user can traverse the maze with the arrow keys
  public void onKeyEvent(String key) {
    // b / breadth first
    if (key.equals("b")) {
      this.searching = true;
      this.breadthFirst = true;
    }
    // d / depth first
    if (key.equals("d")) {
      this.searching = true;
      this.depthFirst = true;
    }
    // r / reset
    if (key.equals("r")) {
      this.preferHorizontal = false;
      this.preferVertical = false;
      this.reset();
    }
    // h / horizontal bias
    if (key.equals("h")) {
      this.preferHorizontal = true;
      this.preferVertical = false;
      this.reset();
    }
    // v / vertical bias
    if (key.equals("v")) {
      this.preferVertical = true;
      this.preferHorizontal = false;
      this.reset();
    }
    // backspace / clear
    if (key.equals("backspace")) {
      this.preferHorizontal = false;
      this.preferVertical = false;
      this.clear();
    }
    // enter / solution
    if (key.equals("enter")) {
      this.searching = false;
      Cell cell = this.board.get(height - 1).get(length - 1);
      if (this.doneBFS) {
        this.breadthFirst();
        while (!(cell.equals(this.board.get(0).get(0)))) {
          cell.changeColor(Color.blue);
          cell = cameFromCells.get(cell);
        }
        this.doneBackTracking = true;
        this.tick = 0;
      }
      if (this.doneDFS) {
        this.depthFirst();
        while (!(cell.equals(this.board.get(0).get(0)))) {
          cell.changeColor(Color.blue);
          cell = cameFromCells.get(cell);
        }
        this.doneBackTracking = true;
        this.tick = 0;
      }
    }
    // traversing
    if (!this.searching) {
      Cell current = this.board.get(curY).get(curX);
      Cell end = this.board.get(height - 1).get(length - 1);
      if (key.equals("left") && (!current.left)) {
        current.changeColor(Color.LIGHT_GRAY);
        curX = curX - 1;
        current = this.board.get(curY).get(curX);
        current.changeColor(Color.gray);
      }
      if (key.equals("right") && (!current.right)) {
        current.changeColor(Color.LIGHT_GRAY);
        curX = curX + 1;
        current = this.board.get(curY).get(curX);
        if (current.equals(end)) {
          this.completed = true;
        }
        current.changeColor(Color.gray);
      }
      if (key.equals("up") && (!current.top)) {
        current.changeColor(Color.LIGHT_GRAY);
        curY = curY - 1;
        current = this.board.get(curY).get(curX);
        current.changeColor(Color.gray);
      }
      if (key.equals("down") && (!current.bottom)) {
        current.changeColor(Color.LIGHT_GRAY);
        curY = curY + 1;
        current = this.board.get(curY).get(curX);
        if (current.equals(end)) {
          this.completed = true;
        }
        current.changeColor(Color.gray);
      }
    }
  }

  // handles each tick
  public void onTick() {
    if (this.searching) {
      if (!visitedBFS.isEmpty()) {
        if (this.tick < this.visitedBFS.size()) {
          Cell displayCell = this.visitedBFS.get(this.tick);
          displayCell.changeColor(Color.cyan);
          this.tick++;
        }
      }
      if (!visitedDFS.isEmpty()) {
        if (this.tick < this.visitedDFS.size()) {
          Cell displayCell = this.visitedDFS.get(this.tick);
          displayCell.changeColor(Color.cyan);
          this.tick++;
        }
      }
      if (!this.end) {
        if (this.breadthFirst) {
          if (this.breadthFirst()) {
            this.end = true;
          }
        }
        if (this.depthFirst) {
          if (this.depthFirst()) {
            this.end = true;
          }
        }
      }
    }
  }

  // resets with a new random maze
  public void reset() {
    this.board = new ArrayList<ArrayList<Cell>>();
    for (int row = 0; row < height; row = row + 1) {
      board.add(new ArrayList<Cell>());
      for (int column = 0; column < length; column = column + 1) {
        board.get(row).add(new Cell(column, row));
      }
    }
    this.edges = new ArrayList<Edge>();
    this.addEdges();
    this.mst = this.kruskal();
    for (Edge e : this.mst) {
      if (e.isHorizontal()) {
        e.from.right = false;
        e.to.left = false;
      }
      else {
        e.from.bottom = false;
        e.to.top = false;
      }
    }
    this.visitedBFS = new ArrayList<Cell>();
    this.visitedDFS = new ArrayList<Cell>();
    this.breadthFirst = false;
    this.depthFirst = false;
    this.searching = false;
    this.end = false;
    this.tick = 0;
    this.cameFromCells = new HashMap<Cell, Cell>();
    this.doneBackTracking = false;
    this.curX = 0;
    this.curY = 0;
    this.doneBFS = false;
    this.doneDFS = false;
    this.completed = false;
  }

  // clears the board without creating a new random maze
  public void clear() {
    for (int row = 0; row < height; row = row + 1) {
      for (int column = 0; column < length; column = column + 1) {
        this.board.get(row).get(column).changeColor(Color.white);
      }
    }
    this.visitedBFS = new ArrayList<Cell>();
    this.visitedDFS = new ArrayList<Cell>();
    this.breadthFirst = false;
    this.depthFirst = false;
    this.searching = false;
    this.end = false;
    this.tick = 0;
    this.cameFromCells = new HashMap<Cell, Cell>();
    this.doneBackTracking = false;
    this.curX = 0;
    this.curY = 0;
    this.doneBFS = false;
    this.doneDFS = false;
    this.completed = false;
  }
}

// examples and tests
class ExamplesMaze {
  static int SCALE = 10;
  Random zeroSeed;
  Random oneSeed;
  MazeWorld testerWorld;
  MazeWorld smallWorld;

  // initial conditions
  void initData() {
    this.zeroSeed = new Random(0);
    this.oneSeed = new Random(1);
    this.testerWorld = new MazeWorld(3, 3, this.zeroSeed);
    this.smallWorld = new MazeWorld(2, 2, this.oneSeed);
  }

  // calls bigBang on the starter maze
  void testMaze(Tester t) {
    MazeWorld starterWorld = new MazeWorld(30, 20);
    starterWorld.bigBang(1000, 700, .01);
  }

  // test for drawCell method
  void testDrawCell(Tester t) {
    initData();
    t.checkExpect(testerWorld.board.get(0).get(0).drawCell(Color.white),
        new RectangleImage(SCALE, SCALE, "solid", Color.white));
    t.checkExpect(testerWorld.board.get(0).get(0).drawCell(Color.blue),
        new RectangleImage(SCALE, SCALE, "solid", Color.blue));
    t.checkExpect(testerWorld.board.get(1).get(2).drawCell(Color.blue),
        new RectangleImage(SCALE, SCALE, "solid", Color.blue));
  }

  // test for cellX method
  void testCellX(Tester t) {
    initData();
    t.checkExpect(testerWorld.board.get(0).get(0).cellX(), 5);
    t.checkExpect(testerWorld.board.get(0).get(0).cellX(),
        SCALE / 2 + testerWorld.board.get(0).get(0).x * SCALE);
    t.checkExpect(testerWorld.board.get(1).get(1).cellX(), 15);
    t.checkExpect(testerWorld.board.get(2).get(1).cellX(), 15);
  }

  // test for cellY method
  void testCellY(Tester t) {
    initData();
    t.checkExpect(testerWorld.board.get(0).get(0).cellY(), 5);
    t.checkExpect(testerWorld.board.get(0).get(0).cellX(),
        SCALE / 2 + testerWorld.board.get(0).get(0).y * SCALE);
    t.checkExpect(testerWorld.board.get(1).get(1).cellY(), 15);
    t.checkExpect(testerWorld.board.get(2).get(1).cellY(), 25);
  }

  // test for changeColor method
  void testChangeColor(Tester t) {
    initData();
    testerWorld.board.get(0).get(0).changeColor(Color.blue);
    t.checkExpect(testerWorld.board.get(0).get(0).color, Color.blue);
    testerWorld.board.get(0).get(0).changeColor(Color.red);
    t.checkExpect(testerWorld.board.get(0).get(0).color, Color.red);
  }

  // test for isHorizontal method
  void testIsHorizontal(Tester t) {
    initData();
    t.checkExpect(testerWorld.board.get(1).get(0).outEdges.get(0).isHorizontal(), true);
    t.checkExpect(testerWorld.board.get(1).get(0).outEdges.get(1).isHorizontal(), false);
    t.checkExpect(testerWorld.board.get(0).get(0).outEdges.get(0).isHorizontal(), true);
  }

  // test for addEdges method
  void testAddEdges(Tester t) {
    initData();
    t.checkExpect(testerWorld.edges.get(0).from, testerWorld.board.get(0).get(1));
    t.checkExpect(testerWorld.edges.get(0).to, testerWorld.board.get(0).get(2));
    t.checkExpect(testerWorld.edges.size(), 12);
    testerWorld.onKeyEvent("h");
    t.checkExpect(testerWorld.edges.get(0).from, testerWorld.board.get(2).get(1));
    t.checkExpect(testerWorld.edges.get(0).to, testerWorld.board.get(2).get(2));
    t.checkExpect(testerWorld.edges.size(), 12);
  }

  // test for makeScene method
  void testMakeScene(Tester t) {
    initData();
    WorldScene threeByThree = new WorldScene(3 * SCALE, 3 * SCALE);
    // cell in row 0 col 0
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.green), SCALE / 2,
        SCALE / 2);
    // top wall
    threeByThree.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2, 0);
    // left wall
    threeByThree.placeImageXY(new LineImage(new Posn(0, SCALE), Color.black), 0, SCALE / 2);

    // cell in row 0 col 1
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2 * 3,
        SCALE / 2);
    // top wall
    threeByThree.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2 * 3, 0);

    // cell in row 0 col 2
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2 * 5,
        SCALE / 2);
    // top wall
    threeByThree.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2 * 5, 0);

    // cell in row 1 col 0
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2,
        SCALE / 2 * 3);
    // top wall
    threeByThree.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2, SCALE);
    // left wall
    threeByThree.placeImageXY(new LineImage(new Posn(0, SCALE), Color.black), 0, SCALE / 2 * 3);

    // cell in row 1 col 1
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2 * 3,
        SCALE / 2 * 3);

    // cell in row 1 col 2
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2 * 5,
        SCALE / 2 * 3);
    // top wall
    threeByThree.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2 * 5, SCALE);

    // cell in row 2 col 0
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2,
        SCALE / 2 * 5);
    // left wall
    threeByThree.placeImageXY(new LineImage(new Posn(0, SCALE), Color.black), 0, SCALE / 2 * 5);

    // cell in row 2 col 1
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2 * 3,
        SCALE / 2 * 5);
    // left wall
    threeByThree.placeImageXY(new LineImage(new Posn(0, SCALE), Color.black), SCALE, SCALE / 2 * 5);

    // cell in row 2 col 2
    threeByThree.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.magenta),
        SCALE / 2 * 5, SCALE / 2 * 5);
    // top wall
    threeByThree.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2 * 5,
        SCALE * 2);

    t.checkExpect(testerWorld.makeScene(), threeByThree);

    WorldScene twoByTwo = new WorldScene(2 * SCALE, 2 * SCALE);
    // cell in row 0 col 0
    twoByTwo.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.green), SCALE / 2,
        SCALE / 2);
    // top wall
    twoByTwo.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2, 0);
    // left wall
    twoByTwo.placeImageXY(new LineImage(new Posn(0, SCALE), Color.black), 0, SCALE / 2);

    // cell in row 1 col 0
    twoByTwo.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2,
        SCALE / 2 * 3);
    // left wall
    twoByTwo.placeImageXY(new LineImage(new Posn(0, SCALE), Color.black), 0, SCALE / 2 * 3);

    // cell in row 0 col 1
    twoByTwo.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.white), SCALE / 2 * 3,
        SCALE / 2);
    // top wall
    twoByTwo.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2 * 3, 0);

    // cell in row 1 col 1
    twoByTwo.placeImageXY(new RectangleImage(SCALE, SCALE, "solid", Color.magenta), SCALE / 2 * 3,
        SCALE / 2 * 3);
    // top wall
    twoByTwo.placeImageXY(new LineImage(new Posn(SCALE, 0), Color.black), SCALE / 2 * 3, SCALE);

    t.checkExpect(smallWorld.makeScene(), twoByTwo);

    // vertically biased two by two
    smallWorld.onKeyEvent("v");
    t.checkExpect(smallWorld.board.get(0).get(0).bottom, false);
    t.checkExpect(smallWorld.board.get(0).get(1).bottom, false);
  }

  // test for lastScene method
  void testLastScene(Tester t) {
    initData();
    WorldScene finalScene = new WorldScene(1000, 700);
    finalScene.placeImageXY(new TextImage("Maze is complete!", 20, Color.black), 500, 350);
    finalScene.placeImageXY(new TextImage("Click 'r' to reset", 15, Color.red), 500, 400);
    finalScene.placeImageXY(new TextImage("Click 'backspace' to clear", 15, Color.black), 500, 430);
    testerWorld.completed = true;
    t.checkExpect(testerWorld.lastScene("Maze is complete!"), finalScene);
  }

  // test for kruskal method
  void testKruskal(Tester t) {
    // initial conditions
    initData();
    // kruskal method is called in the constructor in mst field
    t.checkExpect(testerWorld.mst.size(), 8);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(0).get(0).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(0).get(1).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(0).get(1).outEdges.get(1)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(0).get(1).outEdges.get(2)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(0).get(2).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(1).get(0).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(1).get(0).outEdges.get(1)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(1).get(1).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(1).get(1).outEdges.get(1)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(1).get(1).outEdges.get(2)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(1).get(1).outEdges.get(3)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(1).get(2).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(2).get(0).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(2).get(1).outEdges.get(0)), true);
    t.checkExpect(testerWorld.mst.contains(testerWorld.board.get(2).get(2).outEdges.get(0)), true);

  }

  // test for breadthFirst method
  void testBreadthFirst(Tester t) {
    initData();
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.visitedBFS.size(), 0);
    t.checkExpect(testerWorld.breadthFirst(), true);
    testerWorld.onKeyEvent("b");
    t.checkExpect(testerWorld.breadthFirst, true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(0).get(0)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(0).get(1)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(0).get(2)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(1).get(0)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(1).get(1)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(1).get(2)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(2).get(0)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(2).get(1)), true);
    t.checkExpect(testerWorld.visitedBFS.contains(testerWorld.board.get(2).get(2)), true);
  }

  // test for depthFirst method
  void testDepthFirst(Tester t) {
    initData();
    t.checkExpect(testerWorld.depthFirst, false);
    t.checkExpect(testerWorld.visitedDFS.size(), 0);
    t.checkExpect(testerWorld.depthFirst(), true);
    testerWorld.onKeyEvent("d");
    t.checkExpect(testerWorld.depthFirst, true);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(0).get(0)), true);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(0).get(1)), true);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(0).get(2)), false);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(1).get(0)), false);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(1).get(1)), true);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(1).get(2)), false);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(2).get(0)), false);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(2).get(1)), true);
    t.checkExpect(testerWorld.visitedDFS.contains(testerWorld.board.get(2).get(2)), true);
  }

  // test for onKeyEvent method
  void testOnKeyEvent(Tester t) {
    initData();
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, false);
    t.checkExpect(testerWorld.searching, false);
    testerWorld.onKeyEvent("b");
    t.checkExpect(testerWorld.breadthFirst, true);
    t.checkExpect(testerWorld.depthFirst, false);
    t.checkExpect(testerWorld.searching, true);
    initData();
    testerWorld.onKeyEvent("d");
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, true);
    t.checkExpect(testerWorld.searching, true);
    testerWorld.onKeyEvent("r");
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, false);
    t.checkExpect(testerWorld.searching, false);
    testerWorld.onKeyEvent("d");
    testerWorld.onKeyEvent("backspace");
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, false);
    t.checkExpect(testerWorld.searching, false);
    initData();
    testerWorld.onKeyEvent("d");
    testerWorld.doneDFS = true;
    testerWorld.onKeyEvent("enter");
    t.checkExpect(testerWorld.board.get(0).get(1).color, Color.blue);
    t.checkExpect(testerWorld.board.get(1).get(1).color, Color.blue);
    t.checkExpect(testerWorld.board.get(2).get(1).color, Color.blue);
    t.checkExpect(testerWorld.doneBackTracking, true);
  }

  // test for onTick method
  void testOnTick(Tester t) {
    initData();
    t.checkExpect(testerWorld.visitedBFS.isEmpty(), true);
    t.checkExpect(testerWorld.end, false);
    testerWorld.onKeyEvent("b");
    testerWorld.onTick();
    t.checkExpect(testerWorld.visitedBFS.isEmpty(), false);
    t.checkExpect(testerWorld.end, true);
  }

  // test for reset method
  void testReset(Tester t) {
    initData();
    testerWorld.onKeyEvent("d");
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, true);
    t.checkExpect(testerWorld.searching, true);
    testerWorld.reset();
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, false);
    t.checkExpect(testerWorld.searching, false);
  }

  // test for clear method
  void testClear(Tester t) {
    initData();
    testerWorld.onKeyEvent("d");
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, true);
    t.checkExpect(testerWorld.searching, true);
    testerWorld.clear();
    t.checkExpect(testerWorld.breadthFirst, false);
    t.checkExpect(testerWorld.depthFirst, false);
    t.checkExpect(testerWorld.searching, false);
    t.checkExpect(testerWorld.board.get(0).get(0).color, Color.white);
    t.checkExpect(testerWorld.board.get(1).get(0).color, Color.white);
  }
}
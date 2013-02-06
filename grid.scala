/* Swinemeeper, Copyright (C) 2013 Chris Reuter.  GPL, No Warranty,
   see Copyright.txt. */

package ca.blit.SwineMeeper

import scala.collection.immutable._
import scala.util._

/** Instances of this class represent individual squares of the game's
 *  map.  They contain the square's mutable and immutable state.
 *
 *  @param      hasMine         True if this square has a mine.  Immutable.
 *
 *  @param      adjacentMines   The number of mines adjacent to this square.
 *
 *  @param      adjFlagCount    A function to call when this square is flagged.
 *                              It is used by the owner to keep track of the
 *                              number of flagged squares.
 *
 *  @param      exposeHook      A function to call when this square is
 *                              exposed (i.e. its contents are revealed).  It
 *                              is used by the owner to keep track of the
 *                              number of exposed squares.
 */
class Cell (
  val hasMine:Boolean, 
  val adjacentMines:Int,
  private val adjFlagCount: (Int) => Unit,
  private val exposeHook: () => Unit) {
  
  /** If true, hilight the matching square on display. */
  var hilightSquare = false

  /** True if this square is exposed.  It is initialized false and can
   *  only be set to true. */
  def isExposed = _isExposed
  def isExposed_= (e:Boolean) {
    if (!_isFlagged && e) {
      _isExposed = true
      exposeHook()
    }
  }
  private var _isExposed = false

  /** True if a flag is present.  Cannot be set to true if the
   *  square is exposed. */
  def isFlagged = _isFlagged
  def isFlagged_=(f:Boolean) {
    if (!_isExposed) {
      val incr = if(f) 1 else -1
      if (f != _isFlagged) adjFlagCount(incr)
      _isFlagged = f
    }
  }
  private var _isFlagged = false


  /** Toggle the flag if allowed. */
  def toggleFlagged() = isFlagged = !isFlagged

  /** True if the player hit this mine, ending the game.
   *  Can only be set if hasMine is true. */
  def hasHitMine = _hasHitMine
  def hasHitMine_=(h:Boolean) = if(hasMine) _hasHitMine = h
  private var _hasHitMine = false

  /** Return an ASCII printable version of the square.  Used for
   *  debugging.*/
  def printable():String = {
    if (hasMine) return "!"
    if (adjacentMines == 0) return " "
    return adjacentMines.toString
  }
}


/** Instances of this class contain the entire state of a game.  In
 *  addition to other properties, a GameGrid behaves like a read-only
 *  2-dimensional array of Cell instances.
 *
 *  @param      Width           The width (in cells) of the game grid.
 *
 *  @param      Height          The height (in cells) of the game grid.
 *
 *  @param      NumMines        The number of mines to hide.
 *
 */
class GameGrid (
  val Width:Int, 
  val Height:Int, 
  val NumMines:Int) 
{
  private var gameOver = false
  private var hitMine = false
  private var flagCount = 0
  private var numExposed = 0
  private var cells = initMines()
  private var missCount = 0     // Value is set on end of game

  // Create and return the contents of the map.
  private def initMines() : Array[Array[Cell]] = {
    var cells =
      Array.fill[Cell](Width, Height)(new Cell(false, 0, adjFlagCount,expHook))
    var count = NumMines

    // Set the mines
    while (count > 0) {
      val x = Random.nextInt(Width)
      val y = Random.nextInt(Height)

      if (!cells(x)(y).hasMine) {
        cells(x)(y) = new Cell(true, 0, adjFlagCount, expHook)
        count = count - 1
      }
    }

    // Initialize the count
    for (x <- 0 to Width-1; y <- 0 to Height-1) {
      var mines = 0
      for ((nx,ny) <- spotsAdjacentTo(x, y)) {
        mines = mines + (if (cells(nx)(ny).hasMine) 1 else 0)
      }
      cells(x)(y) = new Cell(cells(x)(y).hasMine, mines, adjFlagCount, expHook)
	}
   
    return cells
  }

  /** Reset this objec to represent a new game. */
  def reset() {
    gameOver = false
    hitMine = false
    cells = initMines()
    flagCount = 0
    numExposed = 0
  }

  // Return a list of (x,y) tuples containing the coordinates of all
  // spots legally within the GameGrid that are adjacent to x and y.
  private def spotsAdjacentTo(x:Int, y:Int) : List[(Int,Int)] = {
    val offsets = List((-1,-1), (0, -1), (1, -1),
                       (-1, 0), (0, 0),  (1, 0),
                       (-1, 1), (0, 1),  (1, 1))

    return for {
      (ox, oy) <- offsets
      if x + ox >= 0 && x + ox < Width
      if y + oy >= 0 && y + oy < Height
    } yield (x+ox, y+oy)
  }

  /** Return a multi-line ASCII representation of this grid.
   *  Used for debugging. */
  def printOut() = {
    for (y <- List.range(0,Height-1)) {
      for (x <- List.range(0,Width-1)) {
        print(cells(x)(y).printable)
      }
      println("")
    }
  }

  /** Retrieve the Cell at (x, y). */
  def apply(x:Int)(y:Int):Cell = cells(x)(y)

  /** Given a point:
   *
   * 1) Mark it exposed (if it isn't)
   * 2) Flag the game as over if it contains a mine
   * 3) Mark adjacent squares as numbered
   * 4) Recursively select adjacent squares if the count == 0
   * 5) Return the set of cells to update in the display.
   */
  def selectSquare (
    x:Int, 
    y:Int,
    skipMines:Boolean = false
  ) : HashSet[(Int,Int)] = {
    val cell = cells(x)(y)
    var result = HashSet.empty[(Int,Int)]

    // If this is exposed, we're done
    if (cell.isExposed || cell.isFlagged) return result

    // Detect if this is a mine and end the game if so
    if (cell.hasMine) {
      if (skipMines) return result

      hitMine = true
      cell.hasHitMine = true
      val updates = setToLosingState()
      return updates      
    }

    // Mark as exposed
    cell.isExposed = true
    result += Tuple2(x,y)

    // And we're done, unless this one has zero adjacent mines
    if (cell.adjacentMines != 0) return result

    // Display the numbers and recursively expose this cell
    for ( (ax, ay) <- spotsAdjacentTo(x,y) ) {
      result ++= selectSquare(ax,ay, true)
    }

    return result
  }

  // Expose all cells and return their coordinates for updating.  Also set 
  // missCount to the number of undetected mines.
  private def setToLosingState() : HashSet[(Int,Int)] = {
    missCount = NumMines

    var result = HashSet.empty[(Int,Int)]
    for (y <- 0 until Height;
         x <- 0 until Width) {
      val cell = cells(x)(y)
      result += Tuple2(x,y)

      if (cell.isFlagged && cell.hasMine) missCount = missCount - 1

      cell.hilightSquare = cell.isFlagged
      cell.isFlagged = false    // So isExposed can be set to true
      cell.isExposed = true
    }

    return result
  }

  // Queries about game status
  def isOver = youLose || youWin
  def youLose = hitMine
  def youWin = flagCount == NumMines && allSafeExposed
  def allSafeExposed = (Width*Height - NumMines) == numExposed

  // Return the number of unflagged mines at the end of the game.
  // *May not* be called while the game is still in progress.
  def numUnflaggedMines = {
    assert(isOver)
    missCount
  }
  
  // Maintain flag count.  adjFlagCount is passed to the Cells to do
  // the dirty work.
  def minesNotFound = NumMines - flagCount
  private def adjFlagCount(i:Int) {
    flagCount = flagCount + i
  }

  // Increment the exposed counter
  private def expHook() {
    numExposed = numExposed + 1
  }
}



/* Swinemeeper, Copyright (C) 2013 Chris Reuter.  GPL, No Warranty,
   see Copyright.txt. */

package ca.blit.SwineMeeper

import swing._
import swing.event._
import scala.collection.immutable._

import GridBagPanel._
import java.awt.Insets
import javax.swing._
import javax.swing.border._
import java.awt.Color


/** Instances of this class display a grid square.  They respond to clicks by
 *  calling a hook function.  Different configurations are set by calling
 *  methods; instance do not directly query their associated Cells.
 *
 *  @param      leftClick       The function to call when left-clicked.
 *
 *  @param      rightClick      The function to call when right-clicked.
 * 
 */
class GridButton (
  val leftClick: () => Unit,
  val rightClick: () => Unit
) extends Label {
  private val EXTENT = 16 + 2
  private val BROWN = new Color(129, 69, 9)

  // Icons
  private val mine = getIcon("mine.png");
  private val untouched = getIcon("untouched.png")
  private val flag = getIcon("flag.png");
  private val numbers =
    ( (0 to 8).map{x => getIcon(x.toString + ".png")} ).toArray

  (List(mine, untouched) ++ numbers).
    foreach{ i => assert(i.getIconWidth > 0, println(i.toString) ) }

  maximumSize = new Dimension(EXTENT+1, EXTENT+1)
  minimumSize = maximumSize
  preferredSize = maximumSize   // this seems to be what does the sizing
  opaque = true                 // and it needs to be opaque.  Simple!

  reset()
  
  listenTo(mouse.clicks)

  reactions += {
    case MouseClicked(src, point, 0, clicks, pops) => leftClick()
    case MouseClicked(src, point, 256, clicks, pops) => rightClick()
  } 

  private def getIcon(name:String) = Swing.Icon(getClass.getResource(name))

  /** Reset to the original appearance (empty, unexposed square). */
  def reset() {
    background = Color.GREEN
    border = Swing.LineBorder (Color.BLACK, 1)
    icon = untouched
  }

  // Colour as exposed.
  private def expose() {
    border = Swing.EmptyBorder
    background = BROWN
  }

  /** Set exposed to show the icon for 'n' surrounding mines.  ('n' must
   *  be between 0 and 8.) */
  def showNumber(n:Int) {
    icon = numbers(n)
    expose()
  }

  /** Set exposed to show a mine.  If 'exploded' is true, show it exploded
   *  as well. */
  def showMine(exploded:Boolean) {
    expose()
    if (exploded) {
      background = Color.RED
    }
    icon = mine
  }

  /** Set to show as a flagged location.  Should not be called if this
   *  square was previously exposed. */
  def showFlag() {
    icon = flag
  }

  /** Surround the square with a yellow border.  NUST be called *after*
   *  the square is exposed. */ 
  def hilight() {
    border = Swing.LineBorder(Color.YELLOW, 1)
  }
}


/** Instances of MapGrid implement a GUI object containing the grid of
 *  GridButtons which display the game map.  Methods manage updating,
 *  which is done by reading the game states of the matching Cell in the
 *  GameGrid.
 *
 *  @param      width           Width (in cells) of the map.
 *
 *  @param      height          Height (in cells) of the map.
 *
 *  @param      gameGrid        The game state.
 *
 *  @param      leftClickFn     Function to call on grid-square left-click.
 *                              It takes the coordinates of the grid square
 *                              as arguments.
 *
 *  @param      rightClickFn    Function to call on grid-square right-click.
 *                              It takes the coordinates of the grid square
 *                              as arguments.
 */
class MapGrid(private val width: Int,
              private val height: Int,
              private val gameGrid: GameGrid,
              private val leftClickFn: (Int, Int) => Unit,
              private val rightClickFn: (Int, Int) => Unit
            ) extends BoxPanel (Orientation.Vertical) {

  private val gridButtons = Array.ofDim[GridButton](width, height)

  for (y <- 0 until height) {
    contents += new BoxPanel (Orientation.Horizontal) {
      for (x <- 0 until width) {
        var gb = new GridButton(() => leftClickFn(x, y),
                                () => rightClickFn(x,y))
        gridButtons(x)(y) = gb
        contents += gb
      }
    }
  }

  /** Redraw all of the grid squares in the given set of coordinates to
   *  accurately reflect the state of their corresponding GameGrid cells. */
  def update(updates:Set[(Int, Int)]) {
    for ( (updateX, updateY) <- updates) {
      updateAt(updateX, updateY)
    }
  }

  /** Redraw the entire grid, making the squares accurately reflect
   *  the state of their corresponding GameGrid cells. */
  def updateAll() {
    for (x <- 0 until width;
         y <- 0 until height) {
      updateAt(x,y)
    }
  }

  /** Redraw the square at (x, y) to accurately reflect the state of
   *  the corresponding cell in the associated GameGrid. */
  def updateAt(x:Int, y:Int) {
    val cell = gameGrid(x)(y)
    val button = gridButtons(x)(y)

    if (cell.isExposed) {
      if (cell.hasMine) {
        button.showMine(cell.hasHitMine)
      } else {
        button.showNumber(cell.adjacentMines)
      }

      if(cell.hilightSquare) button.hilight()
    } else if (cell.isFlagged) {
      button.showFlag()
    } else {
      button.reset()
    }
  }
}


/** This is the game's main window. */
class GameFrame extends MainFrame {
  private val gridx = 25
  private val gridy = 20
  private val maxMines = 60

  private val gameGrid = new GameGrid(gridx, gridy, maxMines)
  private val mapGrid = new MapGrid(gridx, gridy, gameGrid,
                            selected, toggleFlagged)

  // The controls:

  val restartButton = new Button {
    text = "Restart"
    listenTo(mouse.clicks)
    reactions += {
      case MouseClicked(src, point, 0, clicks, pops) => {
        gameGrid.reset()
        mapGrid.updateAll()
        statusLabel.reset()
        mineCountDisp.updateText()
      }
    }
  }

  val quitButton = new Button {
    text = "Quit"
    listenTo(mouse.clicks)
    reactions += {
      case MouseClicked(src, point, 0, clicks, pops) => Main.quit()
    }
  }

  val statusLabel = new Label {
    def setWin() {
      text = "Winner!"
    }

    def setLose() {
      text = "You lose! :-("
    }

    def reset() {
      text = "Game on!"
    }

	reset()
  }

  val mineCountDisp = new Label {
    def updateText() {
      val count =
        if (gameGrid.youLose)
          gameGrid.numUnflaggedMines 
        else 
          gameGrid.minesNotFound

      text = "%3d".format(count)
    }

    border = new BevelBorder(BevelBorder.LOWERED)
    updateText()
  }


  // Construct the window:
  size = new Dimension(600, 500)
  title = "Swine Meeper"

  contents = new BoxPanel (Orientation.Vertical) {
    contents += new FlowPanel {
      contents += new Label("Pigs:")
      contents += mineCountDisp
    }
    contents += mapGrid
    contents += new FlowPanel (restartButton, statusLabel, quitButton)
  }

  // Implement the left mouse click behaviour.  Specifically, expose the
  // square at (x,y), ending the game if it contains a mine.
  private def selected(x:Int, y:Int) : Unit = {
    val updates = gameGrid.selectSquare(x,y)
    mapGrid.update(updates)

    mineCountDisp.updateText()

    checkGameStatus()
  }

  // Implement the right mouse click behaviour.  Specifically, toggles the
  // flag on unexposed grid squares.
  private def toggleFlagged(x:Int, y:Int) : Unit = {
    gameGrid(x)(y).toggleFlagged
    mapGrid.updateAt(x, y)
    mineCountDisp.updateText()

    checkGameStatus()
  }

  // If the game is over, inform the player that they won or lost.
  private def checkGameStatus() {
    if (!gameGrid.isOver) return

    if (gameGrid.youWin) 
      statusLabel.setWin()
    else
      statusLabel.setLose()
  }

}

/** The entry point. */
object Main extends SimpleSwingApplication {
  def top = new GameFrame
}

  

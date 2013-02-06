SwineMeeper
===========

Overview
--------

SwineMeeper is a game involving burrowing explosive pigs (unlike
similar games, which involve mines and sweeping).

The game shows a grid view of a meadow containing a number of a rare
breed of burrowing pigs, all of which have the unfortunate habit of
exploding when disturbed.  Your job is to locate all of the pigs and
plant flags (by right-clicking) on the squares they occupy.

Digging up a square (by left-clicking) will reveal the number of
adjacent squares containing pigs.  However, digging up an actual pig
will cause it to explode, ending the game.

You win the game by correctly flagging all of the pigs and digging up
all the other squares.



Building and Running
--------------------

To build SwineMeeper, you will need:

   * Scala 2.9.1 or later.  (Other versions may work.)
   * A compatible Java VM.
   * GNU make or equivalent.
   * ImageMagick (or maybe GraphicsMagick--I haven't tried it.)
   * A sufficiently UNIX-ish build environment (for the inline shell
     scripts).


Just clone the source code and type 'make' to build it.

To run it, type

    scala SwineMeeper.jar

Note that you will need Java and Scala to run the resulting jar file.
It's possible to create a redistributable jar that only needs a JVM
but I'm not motivated enough to figure out how.  If you manage it,
feel free to send me a pull request.


Administrata
------------

SwineMeeper is Copyright (C) Chris Reuter 2013 and is free software
redistributable under the terms of the GNU general public license.
See the file 'Copyright.txt' for details.

SwineMeeper is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.




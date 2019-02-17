import java.awt.Color;
import java.util.Random;
import javalib.funworld.*;
import javalib.worldimages.*;
import tester.Tester;

// The main class which represents the world state.
class NBullets extends World {
  int numBullets;
  Random rand;

  NBullets(int numBullets, Random rand) {
    this.numBullets = numBullets;
    this.rand = rand;
  }

  NBullets(int numBullets) {
    this(numBullets, new Random());
  }
  // to get a random number between 0, 50: int random = (int)(Math.random() * 50 +
  // 1);

  public WorldScene makeScene() {
    return getEmptyScene(); // why not new WorldScene(int width, int height)
  }

  public boolean bigBang(int width, int height, double speed) { // speed represents tick rate.
    return true;
  } // should we consider adding an lastScene?
}

abstract class AGamePiece {
  int radius; // Measured in pixels
  Posn coords;
  double velocity;
  Color color;

  AGamePiece(int radius, Posn coords, double velocity, Color color) {
    this.radius = radius;
    this.coords = coords;
    this.velocity = velocity; // should we consider changing velocity to a Posn as opposed to a
                              // double? Bullets don't fly only in one direction
    this.color = color;
  }

  boolean pieceHit(AGamePiece that) {
    return (Math.hypot(this.coords.x - that.coords.x, this.coords.y - that.coords.y)) < (this.radius
        + that.radius);
  }
}

class Ship extends AGamePiece {

  Ship(Posn coords) {
    super(10, coords, 4, Color.CYAN);
  }
}

class Bullet extends AGamePiece {
  Bullet(int radius) {
    super(radius, new Posn(150, 500), 8, Color.PINK); // Why is the position (150,500)? Shouldn't it
                                                      // be (250, 300)
  }

  // Moves a singular bullet by it's velocity
  Bullet moveBullet() {
    return new Bullet(radius, new Posn(this.coords.x, (this.coords.y - 8)), 8, Color.PINK);
    // we have to return a new bullet with the new position. Not sure if we can do
    // this with the current state of our constructor.
  }
}

interface ILoShip {
}

class MtLoShip implements ILoShip {
}

class ConsLoShip implements ILoShip {
  Ship first;
  ILoShip rest;

  ConsLoShip(Ship first, ILoShip rest) {
    this.first = first;
    this.rest = rest;
  }
}

interface ILoBullet {
  ILoBullet moveBullets();
}

class MtLoBullet implements ILoBullet {
  // returns an empty list of bullets bc there are no bullets to move.
  public ILoBullet moveBullets() {
    return this;
  }
}

class ConsLoBullet implements ILoBullet {
  Bullet first;
  ILoBullet rest;

  ConsLoBullet(Bullet first, ILoBullet rest) {
    this.first = first;
    this.rest = rest;
  }

  // moves all bullets in the list
  public ILoBullet moveBullets() {
    return new ConsLoBullet(this.first.moveBullet(), this.rest.moveBullets());
  }

}

// Not sure how examples work with big bang
class ExamplesMyWorldProgram {
  boolean testBigBang(Tester t) {
    NBullets w = new NBullets(10);
    int worldWidth = 500;
    int worldHeight = 800;
    double tickRate = 1;
    return w.bigBang(worldWidth, worldHeight, tickRate);
  }
}

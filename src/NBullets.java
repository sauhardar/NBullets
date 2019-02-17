import java.awt.Color;
import java.util.Random;
import javalib.funworld.*;
import javalib.worldimages.*;
import tester.Tester;

// The main class which represents the world state.
class NBullets extends World {
  int bulletsLeft;
  int shipsDestroyed;
  Random rand;
  ILoBullet loBullets;
  ILoShip loShips;

  NBullets(int bulletsLeft, int shipsDestroyed, Random rand, ILoBullet loBullets, ILoShip loShips) {
    this.bulletsLeft = bulletsLeft;
    this.shipsDestroyed = shipsDestroyed;
    this.rand = rand;
    this.loBullets = loBullets;
    this.loShips = loShips;
  }

  NBullets(int bulletsLeft) {
    this(bulletsLeft, 0, new Random(), new MtLoBullet(), new MtLoShip());
  }
  // to get a random number between 0, 50: int random = (int)(Math.random() * 50 +
  // 1);

  public WorldScene makeScene() {
    WorldScene bullets = this.loBullets.drawBullets(new WorldScene(500, 300));
    WorldScene ships = this.loShips.drawShips(bullets);
    return ships.placeImageXY(writeText(), 10, 270);
  }

  public WorldImage writeText() {
    return new TextImage(
        "Bullets left: " + this.bulletsLeft + "; " + "Ships destroyed: " + this.shipsDestroyed,
        Color.BLACK);
  }

  public boolean bigBang(int width, int height, double speed) { // speed represents tick rate.
    return true;
  }

  public NBullets onKeyEvent(String key) {
    if (key.equals("space")) { // make sure " " is actually the key for space.
      return new NBullets(this.bulletsLeft, this.shipsDestroyed, this.rand,
          new ConsLoBullet(new Bullet(2, new Posn(250, 300), 8, 0), this.loBullets), this.loShips);
    }
    else {
      return this;
    }
  }

  public WorldEnd worldEnds() { //Game ends when bulletsLeft == 0 && is MtLoBullet
    if (this.bulletsLeft == 0) {
      return new WorldEnd(true, this.makeFinalScene()); // write this method for end scene.
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }
}

class Ship {
  int radius; // Measured in pixels
  Posn coords;
  double velocity;
  Color color = Color.CYAN;

  Ship(int radius, Posn coords, double velocity) {
    this.radius = radius;
    this.coords = coords;
    this.velocity = velocity;
  }

  boolean shipHit(Bullet that) {
    return (Math.hypot(this.coords.x - that.coords.x, this.coords.y - that.coords.y)) < (this.radius
        + that.radius);
  }

  boolean isOffScreen() {
    return this.coords.x < 0 || this.coords.x > 500 || this.coords.y < 0 || this.coords.y > 300;
    // Hard-coded width and height; subject to change
  }

  WorldScene drawOneShip(WorldScene ws) {
    return ws.placeImageXY(new CircleImage(this.radius, OutlineMode.SOLID, this.color),
        this.coords.x, this.coords.y);
  }
}

class Bullet {
  int radius; // Measured in pixels
  Posn coords;
  double velocity;
  Color color = Color.PINK;
  int numExplosions;

  Bullet(int radius, Posn coords, double velocity, int numExplosions) {
    this.radius = radius;
    this.coords = coords;
    this.velocity = velocity;
    this.numExplosions = numExplosions;
  }

  // Moves a singular bullet by it's velocity
  Bullet moveBullet(int ithBullet) {
    return new Bullet(radius, this.coords, 8.0, numExplosions);
  }

  boolean isOffScreen() {
    return this.coords.x < 0 || this.coords.x > 500 || this.coords.y < 0 || this.coords.y > 300;
    // Hard-coded width and height; subject to change
  }

  WorldScene drawOneBullet(WorldScene ws) {
    return ws.placeImageXY(new CircleImage(this.radius, OutlineMode.SOLID, this.color),
        this.coords.x, this.coords.y);
  }
}

interface ILoShip {
  ILoShip removeShip(ILoBullet that);

  ILoShip removeOffscreen();

  WorldScene drawShips(WorldScene ws);
}

class MtLoShip implements ILoShip {
  public ILoShip removeShip(ILoBullet that) {
    return this;
  }

  public ILoShip removeOffscreen() {
    return this;
  }

  public WorldScene drawShips(WorldScene ws) {
    return ws;
  }
}

class ConsLoShip implements ILoShip {
  Ship first;
  ILoShip rest;

  ConsLoShip(Ship first, ILoShip rest) {
    this.first = first;
    this.rest = rest;
  }

  public ILoShip removeShip(ILoBullet that) {
    if (that.shipRemove(this.first)) {
      return this.rest.removeShip(that);
    }
    else {
      return new ConsLoShip(this.first, this.rest.removeShip(that));
    }
  }

  public ILoShip removeOffscreen() {
    if (this.first.isOffScreen()) {
      return this.rest.removeOffscreen();
    }
    else {
      return new ConsLoShip(this.first, this.rest.removeOffscreen());
    }
  }

  public WorldScene drawShips(WorldScene ws) {
    return this.rest.drawShips(this.first.drawOneShip(ws));
  }
}

interface ILoBullet {
  ILoBullet moveBullets();

  boolean shipRemove(Ship ship);

  ILoBullet removeOffScreen();

  WorldScene drawBullets(WorldScene ws);
}

class MtLoBullet implements ILoBullet {
  // returns an empty list of bullets because there are no bullets to move.
  public ILoBullet moveBullets() {
    return this;
  }

  public boolean shipRemove(Ship ship) {
    return false;
  }

  public ILoBullet removeOffScreen() {
    return this;
  }

  public WorldScene drawBullets(WorldScene ws) {
    return ws;
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

  public boolean shipRemove(Ship ship) {
    return ship.shipHit(this.first) || this.rest.shipRemove(ship);
  }

  public ILoBullet removeOffScreen() {
    if (this.first.isOffScreen()) {
      return this.rest.removeOffScreen();
    }
    else {
      return new ConsLoBullet(this.first, this.rest.removeOffScreen());
    }
  }

  public WorldScene drawBullets(WorldScene ws) {
    return this.rest.drawBullets(this.first.drawOneBullet(ws)); // Might be other way around
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

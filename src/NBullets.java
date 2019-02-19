import java.awt.Color;
import java.util.Random;
import javalib.funworld.*;
import javalib.worldimages.*;
import tester.Tester;

// The main class which represents the world state.
class NBullets extends World {
  int bulletsLeft;
  int shipsDestroyed;
  ILoBullet loBullets;
  ILoShip loShips;
  int numTicks;

  // Main constructor:
  NBullets(int bulletsLeft, int shipsDestroyed, ILoBullet loBullets, ILoShip loShips,
      int numTicks) {
    if (bulletsLeft < 0) {
      throw new IllegalArgumentException("Not a valid number of bullets");
      // TEST CONSTRUCTOREXCEPTION
    }
    else {
      this.bulletsLeft = bulletsLeft;
      this.shipsDestroyed = shipsDestroyed;
      this.loBullets = loBullets;
      this.loShips = loShips;
      this.numTicks = numTicks;
    }
  }

  // VARIABLES:
  static final int WIDTH = 500;
  static final int HEIGHT = 300;

  static final int INITIALBSIZE = 2;
  static final int BSPEED = 8;
  static Color BColor = Color.PINK;
  static final Posn INITIALBPOS = new Posn(250, 300);
  static final int BINCREMENTSIZE = 2;
  static final int BMAXSIZE = 10;

  static final Posn TEXTPOS = new Posn(110, 290);
  static final int TEXTSIZE = 13;
  static Color TEXTCOLOR = Color.BLACK;

  static final int SSPAWNRATE = 28;
  static final int SHIPSIZE = 10;
  static Color SHIPCOLOR = Color.CYAN;

  // Convenience constructor that only takes in the bullets left.
  // User initiates game with this constructor.
  NBullets(int bulletsLeft) {
    this(bulletsLeft, 0, new MtLoBullet(), new MtLoShip(), 0);
  }

  // Visualizes the world. Draws the text on top of the ships on top
  // of the bullets on top of the canvas.
  public WorldScene makeScene() {
    WorldScene bullets = this.loBullets
        .drawBullets(new WorldScene(NBullets.WIDTH, NBullets.HEIGHT));
    WorldScene ships = this.loShips.drawShips(bullets);
    return ships.placeImageXY(writeText(), NBullets.TEXTPOS.x, NBullets.TEXTPOS.y);
  }

  // Displays the bullets the player has left and the score.
  public WorldImage writeText() {
    return new TextImage(
        ("Bullets left: " + this.bulletsLeft + "; " + "Ships destroyed: " + this.shipsDestroyed),
        NBullets.TEXTSIZE, NBullets.TEXTCOLOR);
  }

  // Shoots a new bullet from the bottom of the screen when space bar is pressed.
  public NBullets onKeyEvent(String key) {
    if (key.equals(" ") && this.bulletsLeft != 0) { // if the user presses space while the last
                                                    // bullet is still on screen, will throw an
                                                    // IllegalArgumentException
      return new NBullets(this.bulletsLeft - 1, this.shipsDestroyed, new ConsLoBullet(
          new Bullet(NBullets.INITIALBSIZE, NBullets.INITIALBPOS, new Posn(0, NBullets.BSPEED), 1),
          this.loBullets), this.loShips, this.numTicks);
    }
    else {
      return this;
    }
  }

  // Ends the world program when the bullets left is 0 and there are
  // no bullets on the screen (which is the same as the ILoBullet being empty)
  public WorldEnd worldEnds() {
    if (this.bulletsLeft == 0 && this.loBullets.noneLeft()) { // Game is ending prematurely
      return new WorldEnd(true, this.makeScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  // Updates the world by spawning ships at the given rate and incrementing the
  // ticks at each tick, removing ships and bullets that are off screen, removing
  // ships and bullets that have come in contact, and moving the game pieces.
  public World onTick() {
    NBullets temp = new NBullets(this.bulletsLeft,
        this.loShips.countHits(this.shipsDestroyed, this.loBullets),
        loBullets.removeOffScreen().removeBulletIfHit(loShips).moveBullets(), // .removeBulletIfHit()
                                                                              // has to come before
                                                                              // moveBullets()
        loShips.removeOffscreen().removeShip(loBullets).moveShips(), this.numTicks);

    if (numTicks % NBullets.SSPAWNRATE == 0) {
      numTicks++;

      Random r = new Random();
      int numToSpawn = r.nextInt(4);

      return new NBullets(temp.bulletsLeft, temp.shipsDestroyed, temp.loBullets,
          temp.loShips.spawnShips(numToSpawn), this.numTicks);
    }
    else {
      numTicks++;
      return new NBullets(temp.bulletsLeft, temp.shipsDestroyed, temp.loBullets, temp.loShips,
          this.numTicks);
    }
  }
}

// The targets that the user is trying to hit.
class Ship {
  int radius = NBullets.SHIPSIZE; // Measured in pixels
  Posn coords;
  double velocity; // Pixels per tick
  Color color = NBullets.SHIPCOLOR;

  Ship(Posn coords, double velocity) {
    this.coords = coords;
    this.velocity = velocity;
  }

  // Determines if a ship has been hit by a given bullet.
  boolean shipHit(Bullet that) {
    return (Math.hypot(this.coords.x - that.coords.x, this.coords.y - that.coords.y)) < (this.radius
        + that.radius);
  }

  // Determines if a ship is off the screen.
  boolean isOffScreen() {
    return this.coords.x < 0 || this.coords.x > NBullets.WIDTH || this.coords.y < 0
        || this.coords.y > NBullets.HEIGHT;
  }

  // Visualizes one ship at its position
  WorldScene drawOneShip(WorldScene ws) {
    return ws.placeImageXY(new CircleImage(this.radius, OutlineMode.SOLID, this.color),
        this.coords.x, this.coords.y);
  }

  // Moves the ship by its own velocity
  Ship moveShip() {
    return new Ship(new Posn((int) (this.coords.x + this.velocity), this.coords.y), this.velocity);
  }
}

// The user's ammunition
class Bullet {
  int radius; // Measured in pixels
  Posn coords;
  Posn velocity;
  Color color = NBullets.BColor;
  int numExplosions;

  Bullet(int radius, Posn coords, Posn velocity, int numExplosions) {
    this.radius = radius;
    this.coords = coords;
    this.velocity = velocity;
    this.numExplosions = numExplosions;
  }

  // Determines if a bullet is off screen
  boolean isOffScreen() {
    return this.coords.x < 0 || this.coords.x > NBullets.WIDTH || this.coords.y < 0
        || this.coords.y > NBullets.HEIGHT;
    // Hard-coded width and height; subject to change
  }

  // Visualizes one bullet as a circle at its given position
  WorldScene drawOneBullet(WorldScene ws) {
    return ws.placeImageXY(new CircleImage(this.radius, OutlineMode.SOLID, this.color),
        this.coords.x, this.coords.y);
  }

  // Moves one bullet by its given velocity
  Bullet moveBullet() {
    return new Bullet(this.radius,
        new Posn(this.coords.x - this.velocity.x, this.coords.y - this.velocity.y), this.velocity,
        this.numExplosions);
  }

  // Creates a list of bullets when it comes in contact with a ship depending on
  // the
  // number of explosions the bullet has accumulated
  ILoBullet explodeBullet() {
    return this.explodeBulletHelper(this.numExplosions + 1);
  }

  // daniel add comments here
  // ------------------------------------------------------------------------!!
  ILoBullet explodeBulletHelper(int modExplosions) {
    int degrees = modExplosions * (360 / (this.numExplosions + 1));

    if (modExplosions < 1) {
      return new MtLoBullet();
    }

    if (this.radius < NBullets.BMAXSIZE) { // Restricts bullets from getting larger than 10px
      return new ConsLoBullet(new Bullet(this.radius + NBullets.BINCREMENTSIZE, this.coords,
          new Posn((int) (NBullets.BSPEED * Math.cos(Math.toRadians(degrees))),
              (int) (NBullets.BSPEED * Math.sin(Math.toRadians(degrees)))),
          this.numExplosions + 1), this.explodeBulletHelper(modExplosions - 1));
    }
    else {
      return new ConsLoBullet(new Bullet(this.radius, this.coords,
          new Posn((int) (NBullets.BSPEED * Math.cos(Math.toRadians(degrees))),
              (int) (NBullets.BSPEED * Math.sin(Math.toRadians(degrees)))),
          this.numExplosions + 1), this.explodeBulletHelper(modExplosions - 1));
    }
  }
}

// A list of ships
interface ILoShip {
  ILoShip removeShip(ILoBullet that);

  ILoShip removeOffscreen();

  WorldScene drawShips(WorldScene ws);

  int countHits(int destroyedSoFar, ILoBullet that);

  ILoShip spawnShips(int numToSpawn);

  ILoShip moveShips();

  boolean bulletHit(Bullet target);
}

// A list with no ships
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

  public int countHits(int destroyedSoFar, ILoBullet that) {
    return destroyedSoFar;
  }

  public ILoShip spawnShips(int numToSpawn) {
    if (numToSpawn == 0) {
      return this;
    }
    else {
      if (new Random().nextInt(2) == 0) { // Spawn from left
        return new ConsLoShip(new Ship(
            new Posn(0, new Random().nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), 4),
            this).spawnShips(--numToSpawn);
      }
      else {
        return new ConsLoShip(new Ship(new Posn(NBullets.WIDTH,
            new Random().nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), -4), this)
                .spawnShips(--numToSpawn);
      }
    }
  }

  public ILoShip moveShips() {
    return this;
  }

  public boolean bulletHit(Bullet target) {
    return false;
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

  public int countHits(int destroyedSoFar, ILoBullet that) {
    if (that.shipRemove(this.first)) {
      return this.rest.countHits(destroyedSoFar + 1, that);
    }
    else {
      return this.rest.countHits(destroyedSoFar, that);
    }
  }

  public ILoShip spawnShips(int numToSpawn) {
    if (numToSpawn == 0) {
      return this;
    }
    else {
      if (new Random().nextInt(2) == 0) { // Spawn from left
        return new ConsLoShip(new Ship(
            new Posn(0, new Random().nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), 4),
            this).spawnShips(--numToSpawn);
      }
      else {
        return new ConsLoShip(new Ship(
            new Posn(500, new Random().nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), -4),
            this).spawnShips(--numToSpawn);
      }
    }
  }

  public ILoShip moveShips() {
    return new ConsLoShip(this.first.moveShip(), this.rest.moveShips());
  }

  public boolean bulletHit(Bullet target) {
    return this.first.shipHit(target) || this.rest.bulletHit(target);
  }
}

interface ILoBullet {
  ILoBullet moveBullets();

  ILoBullet removeBulletIfHit(ILoShip that);

  boolean shipRemove(Ship ship);

  ILoBullet removeOffScreen();

  WorldScene drawBullets(WorldScene ws);

  boolean noneLeft();

  ILoBullet appendTo(ILoBullet other);
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

  public boolean noneLeft() {
    return true;
  }

  // Returns an empty list as because no bullets have hit any ships
  public ILoBullet removeBulletIfHit(ILoShip that) {
    return this;
  }

  public ILoBullet appendTo(ILoBullet other) {
    return other;
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

  public boolean noneLeft() {
    return false;
  }

  public ILoBullet removeBulletIfHit(ILoShip that) {
    if (that.bulletHit(this.first)) {
      return this.first.explodeBullet().appendTo(this.rest.removeBulletIfHit(that));
    }
    else {
      return new ConsLoBullet(this.first, this.rest.removeBulletIfHit(that));
    }
  }

  public ILoBullet appendTo(ILoBullet other) {
    return this.rest.appendTo(new ConsLoBullet(this.first, other));
  }
}

class ExamplesNBullets {
  ILoBullet exampleBulls = new MtLoBullet();
  ILoShip exampleShips = new MtLoShip();
  boolean testNBullets(Tester t) {
    return t.checkConstructorException(new IllegalArgumentException("Not a valid number of "
        + "bullets"), "NBullets", -1, 10, exampleBulls, exampleShips, 10)
        && t.checkConstructorException(new IllegalArgumentException("Not a valid number of "
            + "bullets"), "NBullets", -10, 230, exampleBulls, exampleShips, 430);
  }
  
  boolean testBulletMethods(Tester t) {
    Bullet b1 = new Bullet(2, new Posn(50, 50), new Posn(0, 8), 0);
    Bullet b2 = new Bullet(2, new Posn(50, 42), new Posn(0, 8), 0);
    Bullet b3 = new Bullet(2, new Posn(100, 100), new Posn(-10, 10), 5);
    Bullet b4 = new Bullet(2, new Posn(110, 90), new Posn(-10, 10), 5);
    Bullet b5 = new Bullet(2, new Posn(1100, 90), new Posn(-10, 10), 5);
    Bullet b6 = new Bullet(12, new Posn(1100, 90), new Posn(-10, 10), 5);
    WorldScene ws = new WorldScene(500, 300);
    return t.checkExpect(b1.isOffScreen(), false) && t.checkExpect(b5.isOffScreen(), true)
        && t.checkExpect(b1.drawOneBullet(ws),
            ws.placeImageXY(new CircleImage(b1.radius, OutlineMode.SOLID, Color.PINK), b1.coords.x,
                b1.coords.y))
        && t.checkExpect(b2.drawOneBullet(ws),
            ws.placeImageXY(new CircleImage(b2.radius, OutlineMode.SOLID, Color.PINK), b2.coords.x,
                b2.coords.y))
        && t.checkExpect(b1.moveBullet(), b2) && t.checkExpect(b3.moveBullet(), b4)
        && t.checkExpect(b1.explodeBullet(), b1.explodeBulletHelper(1))
        && t.checkExpect(b5.explodeBullet(), b5.explodeBulletHelper(6))
        && t.checkExpect(b1
            .explodeBulletHelper(0), new MtLoBullet())
        && t.checkExpect(b5.explodeBulletHelper(5),
            new ConsLoBullet(
                new Bullet(4, b5.coords,
                    new Posn(
                        (int) (NBullets.BSPEED
                            * Math.cos(Math.toRadians(5 * (360 / (b5.numExplosions + 1))))),
                        (int) (NBullets.BSPEED
                            * Math.sin(Math.toRadians(5 * (360 / (b5.numExplosions + 1)))))),
                    6),
                b5.explodeBulletHelper(4)))
        && t.checkExpect(b6.explodeBulletHelper(5),
            new ConsLoBullet(
                new Bullet(12, b6.coords,
                    new Posn(
                        (int) (NBullets.BSPEED
                            * Math.cos(Math.toRadians(5 * (360 / (b6.numExplosions + 1))))),
                        (int) (NBullets.BSPEED
                            * Math.sin(Math.toRadians(5 * (360 / (b6.numExplosions + 1)))))),
                    6),
                b6.explodeBulletHelper(4)));
  }

  boolean testBigBang(Tester t) {
    NBullets w = new NBullets(10);
    int worldWidth = 500;
    int worldHeight = 300;
    double tickRate = 1.0 / 28.0;
    return w.bigBang(worldWidth, worldHeight, tickRate);
  }
}

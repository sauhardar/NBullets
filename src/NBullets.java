import java.awt.Color;
import java.util.Random;
import javalib.funworld.*;
import javalib.worldimages.*;
import tester.Tester;

// The main class which represents the world state.
class NBullets extends World {
  // FINAL VARIABLES:
  static final int WIDTH = 500;
  static final int HEIGHT = 300;

  static final int INITIALBSIZE = 2; // Radius in pixels
  static final int BSPEED = 8; // In Pixels per tick
  static Color BColor = Color.PINK;
  static final Posn INITIALBPOS = new Posn(250, 300); // Middle of the bottom the screen
  static final int BINCREMENTSIZE = 2; // Increase radius by 2 pixels every time
  static final int BMAXSIZE = 10; // Max radius of 10 pixels

  static final Posn TEXTPOS = new Posn(110, 290); // Text in bottom left of screen
  static final int TEXTSIZE = 13;
  static Color TEXTCOLOR = Color.BLACK;

  static final int SSPAWNRATE = 28; // Ships should spawn every 28 ticks
  static final int SHIPSIZE = 10; // Radius in pixels
  static Color SHIPCOLOR = Color.CYAN;

  /*
   * TEMPLATE FOR NBULLET
   * FIELDS
   * this.bulletsLeft - int
   * this.shipsDestroyed - int
   * this.loBullets - ILoBullet
   * this.loShips - ILoShip
   * this.numTicks - int
   * this.rand - Random
   *
   * METHODS
   * this.makeScene() - WorldScene
   * this.writeText() - WorldImage
   * this.onKeyEvent(String key) - NBullets
   * this.worldEnds() - WorldEnd
   * this.onTick() - World
   *
   * METHODS ON FIELDS
   * Used methods from ILoShip and ILoBullet (See templates below)
   *
   */

  int bulletsLeft;
  int shipsDestroyed;
  ILoBullet loBullets;
  ILoShip loShips;
  int numTicks;
  Random random;

  // Main constructor:
  NBullets(int bulletsLeft, int shipsDestroyed, ILoBullet loBullets, ILoShip loShips, int numTicks,
      Random random) {
    if (bulletsLeft < 0) {
      throw new IllegalArgumentException("Not a valid number of bullets");
    }
    else {
      this.bulletsLeft = bulletsLeft;
      this.shipsDestroyed = shipsDestroyed;
      this.loBullets = loBullets;
      this.loShips = loShips;
      this.numTicks = numTicks;
      this.random = random;
    }
  }

  // Convenience constructor that only takes in the bullets left.
  // User initiates game with this constructor.
  NBullets(int bulletsLeft) {
    this(bulletsLeft, 0, new MtLoBullet(), new MtLoShip(), 0, new Random((long) 1));
  }

  // Visualizes the world. Draws the text on top of the ships on top
  // of the bullets on top of the canvas.
  public WorldScene makeScene() {
    WorldScene bullets = this.loBullets
        .drawBullets(new WorldScene(NBullets.WIDTH, NBullets.HEIGHT));
    WorldScene ships = this.loShips.drawShips(bullets);
    return ships.placeImageXY(this.writeText(), NBullets.TEXTPOS.x, NBullets.TEXTPOS.y);
  }

  // Displays the bullets the player has left and the score.
  public WorldImage writeText() {
    return new TextImage(
        ("Bullets left: " + this.bulletsLeft + "; " + "Ships destroyed: " + this.shipsDestroyed),
        NBullets.TEXTSIZE, NBullets.TEXTCOLOR);
  }

  // Shoots a new bullet from the bottom of the screen when space bar is pressed.
  // if the user presses space while the last bullet is still on screen,
  // will throw an IllegalArgumentException
  public NBullets onKeyEvent(String key) {
    if (key.equals(" ") && this.bulletsLeft != 0) {
      return new NBullets(this.bulletsLeft - 1, this.shipsDestroyed,
          new ConsLoBullet(new Bullet(NBullets.INITIALBSIZE, NBullets.INITIALBPOS,
              new Posn(0, NBullets.BSPEED), 1), this.loBullets),
          this.loShips, this.numTicks, this.random);
    }
    else {
      return this;
    }
  }

  // Ends the world program when the bullets left is 0 and there are
  // no bullets on the screen (ie. ILoBullet is empty)
  public WorldEnd worldEnds() {
    if (this.bulletsLeft == 0 && this.loBullets.noneLeft()) {
      return new WorldEnd(true, this.makeScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  // Updates the world by spawning ships at the given rate and incrementing the
  // ticks at each tick, removing ships and bullets that are off screen, removing
  // ships and bullets that have come in contact, and moving the game pieces.
  // NOTE: method .removeBulletIfHit() has to come before moveBullets()
  public NBullets onTick() {
    NBullets temp = new NBullets(this.bulletsLeft,
        this.loShips.countHits(this.shipsDestroyed, this.loBullets),
        this.loBullets.removeOffScreen().removeBulletIfHit(this.loShips).moveBullets(),
        this.loShips.removeOffscreen().removeShip(this.loBullets).moveShips(), this.numTicks,
        this.random);

    if (numTicks % NBullets.SSPAWNRATE == 0) {
      numTicks++;

      int numToSpawn = this.random.nextInt(4);

      return new NBullets(temp.bulletsLeft, temp.shipsDestroyed, temp.loBullets,
          temp.loShips.spawnShips(numToSpawn, this.random), this.numTicks, this.random);
    }
    else {
      numTicks++;
      return new NBullets(temp.bulletsLeft, temp.shipsDestroyed, temp.loBullets, temp.loShips,
          this.numTicks, this.random);
    }
  }
}

// The targets that the user is trying to hit.
class Ship {

  /*
   * TEMPLATE FOR SHIP
   * FIELDS
   * this.radius - int
   * this.coords - Posn
   * this.velocity - double
   * this.color - Color
   *
   * METHODS
   * this.shipHit(Bullet that) - boolean
   * this.isOffScreen() - boolean
   * this.drawOneShip(WorldScene ws) - WorldScene
   * this.moveShip() - Ship
   *
   * METHODS ON FIELDS
   * none used
   */

  int radius = NBullets.SHIPSIZE; // Measured in pixels
  Posn coords;
  double velocity; // Pixels per tick
  Color color = NBullets.SHIPCOLOR;

  Ship(Posn coords, double velocity) {
    this.coords = coords;
    this.velocity = velocity;
  }

  // convenience constructor just for tests:
  Ship(Posn coords) {
    this(coords, 4);
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

// The item being used to hit the ships
class Bullet {

  /*
   * TEMPLATE FOR BULLET
   * FIELDS
   * this.radius - int
   * this.coords - Posn
   * this.velocity - Posn
   * this.color - Color
   * this.numExplosions - int
   *
   * METHODS
   * this.isOffScreen() - boolean
   * this.drawOneBullet(WorldScene ws) - WorldScene
   * this.moveBullet() - Bullet
   * this.explodeBullet() - ILoBullet
   * this.explodeBulletHelper(int modExplosions) - ILoBullet
   *
   * METHODS ON FIELDS
   * none used
   */

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

  // convenience constructor just for tests:

  Bullet(Posn coords) {
    this(2, coords, new Posn(8, 8), 0);
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
  // the number of explosions the bullet has accumulated
  ILoBullet explodeBullet() {
    return this.explodeBulletHelper(this.numExplosions + 1);
  }

  // Creates the list of bullets, giving each bullet the correct velocity (ie.
  // direction it travels) based on the number of explosions the original bullet
  // had to determine the correct angle
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
  // Removes a ship if it is hit
  ILoShip removeShip(ILoBullet that);

  // Removes all shhips that are not on the screen
  ILoShip removeOffscreen();

  // Draws a list of ships
  WorldScene drawShips(WorldScene ws);

  // Counts the number of ships hit by bullets
  int countHits(int destroyedSoFar, ILoBullet that);

  // Spawns a random number of ships (between 0-3)
  ILoShip spawnShips(int numToSpawn, Random random);

  // Moves all the ships that are on the screen
  ILoShip moveShips();

  // Determines if a bullet has hit any of the ships
  boolean bulletHit(Bullet target);
}

// A list with no ships
class MtLoShip implements ILoShip {

  /*
   * TEMPLATE FOR MTLOSHIP
   * FIELDS
   * this.SHIP_RANDOM - Random
   *
   * METHODS
   * this.removeShip(ILoBullet that) - ILoShip
   * this.removeOffscreen() - ILoShip
   * this.drawShips(WorldScene ws) - WorldScene
   * this.countHits(int destroyedSoFar, ILoBullet that) - int
   * this.spawnShips(int numToSpawn) - ILoShip
   * this.moveShips() - ILoShip
   * this.bulletHit(Bullet target) - boolean
   *
   * METHODS ON FIELDS
   * this.SHIP_RANDOM.nextInt() - int
   */

  // Removes a ship if it is hit from the empty list
  public ILoShip removeShip(ILoBullet that) {
    return this;
  }

  // Removes a ship if it is offscreen from the empty list
  public ILoShip removeOffscreen() {
    return this;
  }

  // Draws all ships in the empty list
  public WorldScene drawShips(WorldScene ws) {
    return ws;
  }

  // Counts the additional ships that have been hit in the empty list
  public int countHits(int destroyedSoFar, ILoBullet that) {
    return destroyedSoFar;
  }

  // Spawns between 0-3 ships randomly in the middle 5/7 of the screen
  public ILoShip spawnShips(int numToSpawn, Random random) {
    if (numToSpawn == 0) {
      return this;
    }
    else {
      if (random.nextInt(2) == 0) { // Spawn from left
        return new ConsLoShip(
            new Ship(new Posn(0, random.nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), 4),
            this).spawnShips(--numToSpawn, random);
      }
      else {
        return new ConsLoShip(new Ship(
            new Posn(NBullets.WIDTH, random.nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)),
            -4), this).spawnShips(--numToSpawn, random);
      }
    }
  }

  // Moves all ships in the empty list of ships
  public ILoShip moveShips() {
    return this;
  }

  // Determines if any of the ships in the empty list were hit by the bullet
  public boolean bulletHit(Bullet target) {
    return false;
  }
}

class ConsLoShip implements ILoShip {

  /*
   * TEMPLATE FOR CONSLOSHIP
   * FIELDS
   * this.first - Ship
   * this.rest - ILoShip
   * this.SHIP_RANDOM - Random
   *
   * METHODS
   * this.removeShip(ILoBullet that) - ILoShip
   * this.removeOffscreen() - ILoShip
   * this.drawShips(WorldScene ws) - WorldScene
   * this.countHits(int destroyedSoFar, ILoBullet that) - int
   * this.spawnShips(int numToSpawn) - ILoShip
   * this.moveShips() - ILoShip
   * this.bulletHit(Bullet target) - boolean
   *
   * METHODS ON FIELDS
   * Methods of Ship (see template above)
   * this.SHIP_RANDOM.nextInt() - int
   */

  Ship first;
  ILoShip rest;

  ConsLoShip(Ship first, ILoShip rest) {
    this.first = first;
    this.rest = rest;
  }

  // Removes all ships that were hit by bullets
  public ILoShip removeShip(ILoBullet that) {
    if (that.shipRemove(this.first)) {
      return this.rest.removeShip(that);
    }
    else {
      return new ConsLoShip(this.first, this.rest.removeShip(that));
    }
  }

  // Removes all ships that are not on the screen
  public ILoShip removeOffscreen() {
    if (this.first.isOffScreen()) {
      return this.rest.removeOffscreen();
    }
    else {
      return new ConsLoShip(this.first, this.rest.removeOffscreen());
    }
  }

  // Draws all the ships
  public WorldScene drawShips(WorldScene ws) {
    return this.rest.drawShips(this.first.drawOneShip(ws));
  }

  // Counts all additional ships that have been hit
  public int countHits(int destroyedSoFar, ILoBullet that) {
    if (that.shipRemove(this.first)) {
      return this.rest.countHits(destroyedSoFar + 1, that);
    }
    else {
      return this.rest.countHits(destroyedSoFar, that);
    }
  }

  // Spawns between 0-3 ships randomly from both sides
  public ILoShip spawnShips(int numToSpawn, Random random) {
    if (numToSpawn == 0) {
      return this;
    }
    else {
      if (random.nextInt(2) == 0) { // Spawn from left
        return new ConsLoShip(
            new Ship(new Posn(0, random.nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), 4),
            this).spawnShips(--numToSpawn, random);
      }
      else {
        return new ConsLoShip(new Ship(
            new Posn(500, random.nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), -4), this)
                .spawnShips(--numToSpawn, random);
      }
    }
  }

  // Moves all ships in the list
  public ILoShip moveShips() {
    return new ConsLoShip(this.first.moveShip(), this.rest.moveShips());
  }

  // Determines if the given bullet hit any of the ships
  public boolean bulletHit(Bullet target) {
    return this.first.shipHit(target) || this.rest.bulletHit(target);
  }
}

interface ILoBullet {
  // Moves all bullets on the screen
  ILoBullet moveBullets();

  // Removes a bullet if it hit any of the ships
  ILoBullet removeBulletIfHit(ILoShip that);

  // Determines if a ship has been hit and needs to be removed
  boolean shipRemove(Ship ship);

  // Removes all bullets that are off the screen
  ILoBullet removeOffScreen();

  // Draws all the bullets
  WorldScene drawBullets(WorldScene ws);

  // Determines if there are any bullets left on the screen
  boolean noneLeft();

  // Combines two lists of bullets
  ILoBullet appendTo(ILoBullet other);
}

class MtLoBullet implements ILoBullet {

  /*
   * TEMPLATE FOR MTLOBULLET
   * FIELDS
   * None
   *
   * METHODS
   * this.moveBullets() - ILoBullet
   * this.removeBulletIfHit(ILoShip that) - ILoBullet
   * this.shipRemove(Ship ship) - boolean
   * this.removeOffScreen() - ILoBullet
   * this.drawBullets(WorldScene ws) - WorldScene
   * this.noneLeft() - boolean
   * this.appendTo(ILoBullet other) - ILoBullet
   *
   * METHODS ON FIELDS
   * None
   */

  // returns an empty list of bullets because there are no bullets to move.
  public ILoBullet moveBullets() {
    return this;
  }

  // Determines if any ships are hit, returns false since there are no bullets
  public boolean shipRemove(Ship ship) {
    return false;
  }

  // Removes all the bullets that are off the screen
  public ILoBullet removeOffScreen() {
    return this;
  }

  // Draws all the bullets in the empty scene
  public WorldScene drawBullets(WorldScene ws) {
    return ws;
  }

  // Determines if there are any bullets left, returns true since there are not
  public boolean noneLeft() {
    return true;
  }

  // Returns an empty list because no bullets have hit any ships
  public ILoBullet removeBulletIfHit(ILoShip that) {
    return this;
  }

  // Combines the two ILoBullet
  public ILoBullet appendTo(ILoBullet other) {
    return other;
  }
}

// Non-empty list of bullets
class ConsLoBullet implements ILoBullet {

  /*
   * TEMPLATE FOR CONSLOBULLET
   * FIELDS
   * this.first - Bullet
   * this.rest - ILoBullet
   *
   * METHODS
   * this.moveBullets() - ILoBullet
   * this.removeBulletIfHit(ILoShip that) - ILoBullet
   * this.shipRemove(Ship ship) - boolean
   * this.removeOffScreen() - ILoBullet
   * this.drawBullets(WorldScene ws) - WorldScene
   * this.noneLeft() - boolean
   * this.appendTo(ILoBullet other) - ILoBullet
   *
   * METHODS ON FIELDS
   * None
   */

  Bullet first;
  ILoBullet rest;

  ConsLoBullet(Bullet first, ILoBullet rest) {
    this.first = first;
    this.rest = rest;
  }

  // Moves all bullets in the list
  public ILoBullet moveBullets() {
    return new ConsLoBullet(this.first.moveBullet(), this.rest.moveBullets());
  }

  // Determines if a ship has been hit by any of the list of bullets
  public boolean shipRemove(Ship ship) {
    return ship.shipHit(this.first) || this.rest.shipRemove(ship);
  }

  // Removes all bullets that are not on screen
  public ILoBullet removeOffScreen() {
    if (this.first.isOffScreen()) {
      return this.rest.removeOffScreen();
    }
    else {
      return new ConsLoBullet(this.first, this.rest.removeOffScreen());
    }
  }

  // Draws all the bullets in the list
  public WorldScene drawBullets(WorldScene ws) {
    return this.rest.drawBullets(this.first.drawOneBullet(ws)); // Might be other way around
  }

  // Returns false because there are still bullets left
  public boolean noneLeft() {
    return false;
  }

  // Creates a new list of bullets with the exploded bullet that hit
  public ILoBullet removeBulletIfHit(ILoShip that) {
    if (that.bulletHit(this.first)) {
      return this.first.explodeBullet().appendTo(this.rest.removeBulletIfHit(that));
    }
    else {
      return new ConsLoBullet(this.first, this.rest.removeBulletIfHit(that));
    }
  }

  // Adds two lists together
  public ILoBullet appendTo(ILoBullet other) {
    return this.rest.appendTo(new ConsLoBullet(this.first, other));
  }
}

class ExamplesNBullets {
  Bullet b1 = new Bullet(2, new Posn(50, 50), new Posn(0, 8), 0);
  Bullet b2 = new Bullet(2, new Posn(50, 42), new Posn(0, 8), 0);
  Bullet b3 = new Bullet(2, new Posn(100, 100), new Posn(-10, 10), 5);
  Bullet b4 = new Bullet(2, new Posn(110, 90), new Posn(-10, 10), 5);
  Bullet b5 = new Bullet(2, new Posn(1100, 90), new Posn(-10, 10), 5);
  Bullet b6 = new Bullet(12, new Posn(1100, 90), new Posn(-10, 10), 5);
  Bullet b7 = new Bullet(12, new Posn(0, 0), new Posn(-10, 10), 5);

  Ship s1 = new Ship(new Posn(0, 0), 3.0);
  Ship s2 = new Ship(new Posn(30, 120), 0.0);
  Ship s3 = new Ship(new Posn(34, 263), 12.0);
  Ship s4 = new Ship(new Posn(4, 74), 8.0);
  Ship s5 = new Ship(new Posn(500, 600), 10.0);

  ILoBullet emptyBulls = new MtLoBullet();
  ILoBullet someBulls = new ConsLoBullet(this.b1,
      new ConsLoBullet(this.b2, new ConsLoBullet(this.b3,
          new ConsLoBullet(this.b4, new ConsLoBullet(this.b7, new MtLoBullet())))));
  ILoBullet moreBulls = new ConsLoBullet(this.b3, new ConsLoBullet(this.b4,
      new ConsLoBullet(this.b5, new ConsLoBullet(this.b6, new MtLoBullet()))));
  ILoBullet moreBulls1 = new ConsLoBullet(this.b3, new ConsLoBullet(this.b4, new MtLoBullet()));

  ILoShip emptyShips = new MtLoShip();
  ILoShip someShips = new ConsLoShip(this.s1,
      new ConsLoShip(this.s2, new ConsLoShip(this.s3, new ConsLoShip(this.s4, new MtLoShip()))));
  ILoShip someShips1 = new ConsLoShip(this.s2,
      new ConsLoShip(this.s3, new ConsLoShip(this.s4, new MtLoShip())));
  ILoShip moreShips = new ConsLoShip(this.s1, new ConsLoShip(this.s2,
      new ConsLoShip(this.s3, new ConsLoShip(this.s4, new ConsLoShip(this.s5, new MtLoShip())))));
  ILoShip moreShips1 = new ConsLoShip(this.s1,
      new ConsLoShip(this.s2, new ConsLoShip(this.s3, new ConsLoShip(this.s4, new MtLoShip()))));

  WorldScene ws = new WorldScene(500, 300);

  NBullets n5 = new NBullets(5);
  NBullets test2 = new NBullets(10, 5, this.someBulls, this.someShips, 30, new Random((long) 1));

  // Tests for NBullets
  boolean testConstructorException(Tester t) {
    return t.checkConstructorException(
        new IllegalArgumentException("Not a valid number of " + "bullets"), "NBullets", -1, 10,
        emptyBulls, emptyShips, 10, new Random())
        && t.checkConstructorException(
            new IllegalArgumentException("Not a valid number of " + "bullets"), "NBullets", -10,
            230, emptyBulls, emptyShips, 430, new Random());
  }

  boolean testMakeScene(Tester t) {
    WorldScene bulletsN5 = this.n5.loBullets
        .drawBullets(new WorldScene(NBullets.WIDTH, NBullets.HEIGHT));
    WorldScene shipsN5 = this.n5.loShips.drawShips(bulletsN5);

    WorldScene bulletsTest2 = this.test2.loBullets
        .drawBullets(new WorldScene(NBullets.WIDTH, NBullets.HEIGHT));
    WorldScene shipsTest2 = this.test2.loShips.drawShips(bulletsTest2);

    return t.checkExpect(this.n5.makeScene(),
        shipsN5.placeImageXY(this.n5.writeText(), NBullets.TEXTPOS.x, NBullets.TEXTPOS.y))
        && t.checkExpect(this.test2.makeScene(), shipsTest2.placeImageXY(this.test2.writeText(),
            NBullets.TEXTPOS.x, NBullets.TEXTPOS.y));
  }

  boolean testWriteText(Tester t) {
    return t.checkExpect(this.n5.writeText(),
        new TextImage(("Bullets left: " + this.n5.bulletsLeft + "; " + "Ships destroyed: "
            + this.n5.shipsDestroyed), NBullets.TEXTSIZE, NBullets.TEXTCOLOR))
        && t.checkExpect(this.test2.writeText(),
            new TextImage(("Bullets left: " + this.test2.bulletsLeft + "; " + "Ships destroyed: "
                + this.test2.shipsDestroyed), NBullets.TEXTSIZE, NBullets.TEXTCOLOR));
  }

  boolean testOnKey(Tester t) {
    return t.checkExpect(this.n5.onKeyEvent(" "),
        new NBullets(this.n5.bulletsLeft - 1, this.n5.shipsDestroyed,
            new ConsLoBullet(new Bullet(NBullets.INITIALBSIZE, NBullets.INITIALBPOS,
                new Posn(0, NBullets.BSPEED), 1), this.n5.loBullets),
            this.n5.loShips, this.n5.numTicks, this.n5.random))
        && t.checkExpect(this.n5.onKeyEvent("a"), this.n5)
        && t.checkExpect(this.test2.onKeyEvent(" "),
            new NBullets(this.test2.bulletsLeft - 1, this.test2.shipsDestroyed,
                new ConsLoBullet(new Bullet(NBullets.INITIALBSIZE, NBullets.INITIALBPOS,
                    new Posn(0, NBullets.BSPEED), 1), this.test2.loBullets),
                this.test2.loShips, this.test2.numTicks, this.test2.random))
        && t.checkExpect(this.test2.onKeyEvent("a"), this.test2);
  }

  boolean testWorldEnds(Tester t) {
    NBullets zeroLeft = new NBullets(0);
    return t.checkExpect(this.n5.worldEnds(), new WorldEnd(false, this.n5.makeScene()))
        && t.checkExpect(zeroLeft.worldEnds(), new WorldEnd(true, zeroLeft.makeScene()))
        && t.checkExpect(this.test2.worldEnds(), new WorldEnd(false, this.test2.makeScene()));
  }

  boolean testOnTick(Tester t) {
    NBullets n5Temp = new NBullets(this.n5.bulletsLeft,
        this.n5.loShips.countHits(this.n5.shipsDestroyed, this.n5.loBullets),
        n5.loBullets.removeOffScreen().removeBulletIfHit(n5.loShips).moveBullets(),
        n5.loShips.removeOffscreen().removeShip(n5.loBullets).moveShips(), this.n5.numTicks,
        this.n5.random);

    return t.checkExpect(this.n5.onTick(),
        new NBullets(5, 0, new MtLoBullet(),
            new ConsLoShip(new Ship(new Posn(0, 111), 4),
                new ConsLoShip(new Ship(new Posn(0, 111), 4), new MtLoShip())),
            1, new Random((long) 1)));
  }

  boolean testShipHit(Tester t) {
    return t.checkExpect(this.s1.shipHit(new Bullet(new Posn(0, 0))), true)
        && t.checkExpect(this.s1.shipHit(new Bullet(new Posn(100, 100))), false);
  }

  boolean testIsOffScreenShip(Tester t) {
    return t.checkExpect(new Ship(new Posn(-10, 0)).isOffScreen(), true)
        && t.checkExpect(this.s3.isOffScreen(), false);
  }

  boolean testDrawOneShip(Tester t) {
    return t.checkExpect(this.s1.drawOneShip(new WorldScene(500, 300)),
        new WorldScene(500, 300).placeImageXY(
            new CircleImage(this.s1.radius, OutlineMode.SOLID, this.s1.color), this.s1.coords.x,
            this.s1.coords.y))
        && t.checkExpect(this.s2.drawOneShip(new WorldScene(500, 300)),
            new WorldScene(500, 300).placeImageXY(
                new CircleImage(this.s2.radius, OutlineMode.SOLID, this.s2.color), this.s2.coords.x,
                this.s2.coords.y))
        && t.checkExpect(this.s3.drawOneShip(new WorldScene(500, 300)),
            new WorldScene(500, 300).placeImageXY(
                new CircleImage(this.s3.radius, OutlineMode.SOLID, this.s3.color), this.s3.coords.x,
                this.s3.coords.y));
  }

  boolean testMoveShip(Tester t) {
    return t.checkExpect(this.s1.moveShip(),
        new Ship(new Posn((int) (this.s1.coords.x + this.s1.velocity), this.s1.coords.y),
            this.s1.velocity))
        && t.checkExpect(this.s2.moveShip(),
            new Ship(new Posn((int) (this.s2.coords.x + this.s2.velocity), this.s2.coords.y),
                this.s2.velocity))
        && t.checkExpect(this.s3.moveShip(),
            new Ship(new Posn((int) (this.s3.coords.x + this.s3.velocity), this.s3.coords.y),
                this.s3.velocity));
  }

  boolean testIsOffScreenBullet(Tester t) {
    return t.checkExpect(this.b1.isOffScreen(), false)
        && t.checkExpect(new Bullet(new Posn(10, 1000)).isOffScreen(), true);
  }

  boolean testIsOffScreen(Tester t) {
    return t.checkExpect(b1.isOffScreen(), false) && t.checkExpect(b5.isOffScreen(), true);
  }

  boolean testDrawOneBullet(Tester t) {
    return t.checkExpect(b1.drawOneBullet(ws),
        ws.placeImageXY(new CircleImage(b1.radius, OutlineMode.SOLID, Color.PINK), b1.coords.x,
            b1.coords.y))
        && t.checkExpect(b2.drawOneBullet(ws), ws.placeImageXY(
            new CircleImage(b2.radius, OutlineMode.SOLID, Color.PINK), b2.coords.x, b2.coords.y));
  }

  boolean testMoveBullet(Tester t) {
    return t.checkExpect(b1.moveBullet(), b2) && t.checkExpect(b3.moveBullet(), b4);
  }

  boolean testExplodeBullet(Tester t) {
    return t.checkExpect(b1.explodeBullet(), b1.explodeBulletHelper(1))
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

  boolean testRemoveShips(Tester t) {
    return t.checkExpect(this.emptyShips.removeShip(this.emptyBulls), this.emptyShips)
        && t.checkExpect(this.someShips.removeShip(this.emptyBulls), this.someShips)
        && t.checkExpect(this.someShips.removeShip(this.someBulls), this.someShips1);
  }

  boolean testRemoveOffScreenILoShips(Tester t) {
    return t.checkExpect(this.emptyShips.removeOffscreen(), this.emptyShips)
        && t.checkExpect(this.moreShips.removeOffscreen(), this.moreShips1);
  }

  boolean testDrawShips(Tester t) {
    WorldScene ws = new WorldScene(500, 300);
    return t.checkExpect(this.emptyShips.drawShips(ws), ws)
        && t.checkExpect(this.someShips.drawShips(ws), ((ConsLoShip) this.someShips).rest
            .drawShips(((ConsLoShip) this.someShips).first.drawOneShip(ws)));
  }

  boolean testCountHits(Tester t) {
    return t.checkExpect(this.emptyShips.countHits(10, this.emptyBulls), 10)
        && t.checkExpect(this.emptyShips.countHits(10, this.someBulls), 10)
        && t.checkExpect(this.someShips.countHits(10, this.emptyBulls), 10)
        && t.checkExpect(this.someShips.countHits(10, this.someBulls), 11);
  }

  boolean testSpawnShips(Tester t) {
    return t.checkExpect(emptyShips.spawnShips(2, this.n5.random),
        new ConsLoShip(new Ship(new Posn(500, 200), -4),
            new ConsLoShip(new Ship(new Posn(500, 200), -4), new MtLoShip())));
  }

  boolean testMoveShips(Tester t) {
    return t.checkExpect(this.emptyShips.moveShips(), this.emptyShips)
        && t.checkExpect(this.someShips.moveShips(),
            new ConsLoShip(((ConsLoShip) this.someShips).first.moveShip(),
                ((ConsLoShip) this.someShips).rest.moveShips()))
        && t.checkExpect(this.someShips1.moveShips(),
            new ConsLoShip(((ConsLoShip) this.someShips1).first.moveShip(),
                ((ConsLoShip) this.someShips1).rest.moveShips()));
  }

  boolean testBulletHit(Tester t) {
    return t.checkExpect(this.emptyShips.bulletHit(this.b1), false)
        && t.checkExpect(this.someShips.bulletHit(this.b1), false)
        && t.checkExpect(this.someShips1.bulletHit(this.b1), false)
        && t.checkExpect(this.moreShips.bulletHit(this.b7), true);
  }

  boolean testMoveBullets(Tester t) {
    return t.checkExpect(this.emptyBulls.moveBullets(), this.emptyBulls)
        && t.checkExpect(this.someBulls.moveBullets(),
            new ConsLoBullet(((ConsLoBullet) this.someBulls).first.moveBullet(),
                ((ConsLoBullet) this.someBulls).rest.moveBullets()));
  }

  boolean testShipRemove(Tester t) {
    return t.checkExpect(this.emptyBulls.shipRemove(this.s1), false)
        && t.checkExpect(this.someBulls.shipRemove(this.s1), true)
        && t.checkExpect(this.someBulls.shipRemove(this.s4), false);
  }

  boolean testRemoveOffScreen(Tester t) {
    return t.checkExpect(this.emptyBulls.removeOffScreen(), this.emptyBulls)
        && t.checkExpect(this.moreBulls.removeOffScreen(), this.moreBulls1);
  }

  boolean testDrawBullets(Tester t) {
    WorldScene ws = new WorldScene(500, 300);
    return t.checkExpect(this.emptyBulls.drawBullets(ws), ws)
        && t.checkExpect(this.someBulls.drawBullets(ws), ((ConsLoBullet) this.someBulls).rest
            .drawBullets(((ConsLoBullet) this.someBulls).first.drawOneBullet(ws)));
  }

  boolean testBigBang(Tester t) {
    NBullets w = new NBullets(10);
    int worldWidth = 500;
    int worldHeight = 300;
    double tickRate = 1.0 / 28.0;
    return w.bigBang(worldWidth, worldHeight, tickRate);
  }
}

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

  NBullets(int bulletsLeft, int shipsDestroyed, ILoBullet loBullets, ILoShip loShips,
      int numTicks) {
    if (bulletsLeft < 0) {
      throw new IllegalArgumentException("Not a valid number of bullets");
      // TEST CONSTRUCTOREXCEPTION
    }
    else {
      this.bulletsLeft = bulletsLeft;
    }
    this.shipsDestroyed = shipsDestroyed;
    this.loBullets = loBullets;
    this.loShips = loShips;
    this.numTicks = numTicks;
  }

  NBullets(int bulletsLeft) {
    this(bulletsLeft, 0, new MtLoBullet(), new MtLoShip(), 0);
  }
  // to get a random number between 0, 50: int random = (int)(Math.random() * 50 +
  // 1);

  public WorldScene makeScene() {
    WorldScene bullets = this.loBullets.drawBullets(new WorldScene(500, 300));
    WorldScene ships = this.loShips.drawShips(bullets);
    return ships.placeImageXY(writeText(), 110, 290);
  }

  public WorldImage writeText() {
    return new TextImage(
        "Bullets left: " + this.bulletsLeft + "; " + "Ships destroyed: " + this.shipsDestroyed, 13,
        Color.BLACK);
  }

  public NBullets onKeyEvent(String key) {
    if (key.equals(" ")) {
      return new NBullets(this.bulletsLeft - 1, this.shipsDestroyed,
          new ConsLoBullet(new Bullet(2, new Posn(250, 300), new Posn(0, 8), 1), this.loBullets),
          this.loShips, this.numTicks);
    }
    else {
      return this;
    }
  }

  public WorldEnd worldEnds() { // Game ends when bulletsLeft == 0 && is MtLoBullet
    if (this.bulletsLeft == 0 && this.loBullets.noneLeft()) { // Game is ending prematurely
      return new WorldEnd(true, this.makeScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  public World onTick() {
    NBullets temp = new NBullets(this.bulletsLeft,
        this.loShips.countHits(this.shipsDestroyed, this.loBullets),
        loBullets.removeOffScreen().removeBulletIfHit(loShips).moveBullets(),
        loShips.removeOffscreen().removeShip(loBullets).moveShips(), this.numTicks);

    if (numTicks % 28 == 0) {
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

    /*
     * To Do
     * - Move all the bullets, including explosions
     */

  }
}

class Ship {
  int radius = 10; // Measured in pixels
  Posn coords;
  double velocity; // Pixels per tick
  Color color = Color.CYAN;

  Ship(Posn coords, double velocity) {
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

  Ship moveShip() {
    return new Ship(new Posn((int) (this.coords.x + this.velocity), this.coords.y), this.velocity);
  }
}

class Bullet {
  int radius; // Measured in pixels
  Posn coords;
  Posn velocity;
  Color color = Color.PINK;
  int numExplosions;

  Bullet(int radius, Posn coords, Posn velocity, int numExplosions) {
    this.radius = radius;
    this.coords = coords;
    this.velocity = velocity;
    this.numExplosions = numExplosions;
  }

  // Moves a singular bullet by it's velocity
  Bullet moveBullet(int ithBullet) {
    return new Bullet(radius, this.coords, new Posn(0, 8), numExplosions);
  }

  boolean isOffScreen() {
    return this.coords.x < 0 || this.coords.x > 500 || this.coords.y < 0 || this.coords.y > 300;
    // Hard-coded width and height; subject to change
  }

  WorldScene drawOneBullet(WorldScene ws) {
    return ws.placeImageXY(new CircleImage(this.radius, OutlineMode.SOLID, this.color),
        this.coords.x, this.coords.y);
  }

  Bullet moveBullet() {
    return new Bullet(this.radius,
        new Posn(this.coords.x - this.velocity.x, this.coords.y - this.velocity.y), this.velocity,
        this.numExplosions);
  }

  ILoBullet explodeBullet() {
    return this.explodeBulletHelper(this.numExplosions + 1);
  }

  ILoBullet explodeBulletHelper(int modExplosions) {
    int degrees = modExplosions * (360 / (this.numExplosions + 1));

    if (modExplosions < 1) {
      return new MtLoBullet();
    }

    if (this.radius < 10) {
      return new ConsLoBullet(
          new Bullet(this.radius + 2, this.coords,
              new Posn((int) (8 * Math.cos(Math.toRadians(degrees))),   // adding here
                  (int) (8 * Math.sin(Math.toRadians(degrees)))),
              this.numExplosions + 1),
          this.explodeBulletHelper(modExplosions - 1));
    }
    else {
      return new ConsLoBullet(
          new Bullet(this.radius, this.coords,
              new Posn((int) (8 * Math.cos(Math.toRadians(degrees))),
                  (int) (8 * Math.sin(Math.toRadians(degrees)))),
              this.numExplosions + 1),
          this.explodeBulletHelper(modExplosions - 1));
    }
  }
}

interface ILoShip {
  ILoShip removeShip(ILoBullet that);

  ILoShip removeOffscreen();

  WorldScene drawShips(WorldScene ws);

  int countHits(int destroyedSoFar, ILoBullet that);

  ILoShip spawnShips(int numToSpawn);

  ILoShip moveShips();

  boolean bulletHit(Bullet target);
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
        return new ConsLoShip(new Ship(
            new Posn(500, new Random().nextInt((int) (5 / 7.0 * 300)) + (int) (1 / 7.0 * 300)), -4),
            this).spawnShips(--numToSpawn);
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
    return true;
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
  boolean testBigBang(Tester t) {
    NBullets w = new NBullets(10);
    int worldWidth = 500;
    int worldHeight = 300;
    double tickRate = 1.0 / 28.0;
    return w.bigBang(worldWidth, worldHeight, tickRate);
  }
}

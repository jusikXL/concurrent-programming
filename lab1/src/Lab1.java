import TSim.*;

import java.util.concurrent.Semaphore;
import static java.lang.Thread.sleep;

public class Lab1 {
  enum Track {
    DIAMOND_CROSSING,
    RIVER_TRACK,
    STONE_TRACK,
    TOP_METAL_TRACK,
    BOTTOM_METAL_TRACK,
    TOP_SOUTH_STATION,
    BOTTOM_SOUTH_STATION,
    TOP_NORTH_STATION,
    BOTTOM_NORTH_STATION
  };

  Semaphore[] semaphores = new Semaphore[Track.values().length];

  enum SensorCase {
    UP_BEFORE, UP_AFTER, DOWN_BEFORE, DOWN_AFTER;

    public static SensorCase get(boolean direction, boolean active) {
      if (direction && active) {
        return UP_BEFORE;
      } else if (direction && !active) {
        return UP_AFTER;
      } else if (!direction && active) {
        return DOWN_BEFORE;
      } else {
        return DOWN_AFTER;
      }
    }
  }

  public Lab1(int speed1, int speed2) {
    TSimInterface tsi = TSimInterface.getInstance();
    tsi.setDebug(false);

    for (int i = 0; i < Track.values().length; i++) {
      semaphores[i] = new Semaphore(1);
    }

    try {
      tsi.setSpeed(1, speed1);
      tsi.setSpeed(2, speed2);
    } catch (CommandException e) {
      e.printStackTrace();
      System.exit(1);
    }

    class Train implements Runnable {
      int id;
      int speed;

      Train(int id, int speed, Track track) {
        this.id = id;
        this.speed = speed;

        // acquire default semaphore
        semaphores[track.ordinal()] = new Semaphore(0);
      }

      boolean getDirection() {
        return (id == 1 && speed < 0) || (id == 2 && speed > 0);
      }

      void acquire(Track track) throws CommandException, InterruptedException {
        tsi.setSpeed(id, 0); // get ready to be blocked
        semaphores[track.ordinal()].acquire();
        tsi.setSpeed(id, speed);
      }

      void release(Track track) {
        semaphores[track.ordinal()].release();
      }

      void stopTrain() throws CommandException, InterruptedException {
        tsi.setSpeed(id, 0);
        sleep(1000 + (20 * Math.abs(speed)));
        speed = -speed;
        tsi.setSpeed(id, speed);
      }

      @SuppressWarnings("incomplete-switch")
      @Override
      public void run() {
        try {
          while (true) {
            SensorEvent sensor = tsi.getSensor(id);

            int x = sensor.getXpos();
            int y = sensor.getYpos();
            SensorCase sensorCase = SensorCase.get(getDirection(), sensor.getStatus() == SensorEvent.ACTIVE);

            // diamond crossing
            if (x == 6 && y == 6 || x == 10 && y == 5) {
              switch (sensorCase) {
                case UP_AFTER:
                  release(Track.DIAMOND_CROSSING);
                  break;
                case DOWN_BEFORE:
                  acquire(Track.DIAMOND_CROSSING);
                  break;
              }
            }
            // diamond crossing and north station fork
            else if (x == 12 && y == 7) {
              switch (sensorCase) {
                case UP_BEFORE:
                  acquire(Track.DIAMOND_CROSSING);
                  break;
                case UP_AFTER:
                  release(Track.RIVER_TRACK);
                  break;
                case DOWN_BEFORE:
                  release(Track.DIAMOND_CROSSING);
                  acquire(Track.RIVER_TRACK);
                  release(Track.TOP_NORTH_STATION);
                  tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
                  break;
              }
            } else if (x == 12 && y == 8) {
              switch (sensorCase) {
                case UP_BEFORE:
                  release(Track.RIVER_TRACK);
                  acquire(Track.DIAMOND_CROSSING);
                  break;
                case DOWN_AFTER:
                  release(Track.DIAMOND_CROSSING);
                  break;
                case DOWN_BEFORE:
                  acquire(Track.RIVER_TRACK);
                  release(Track.BOTTOM_NORTH_STATION);
                  tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
                  break;
              }
            }
            // south station fork
            else if (x == 7 && y == 11) {
              switch (sensorCase) {
                case UP_BEFORE:
                  acquire(Track.STONE_TRACK);
                  release(Track.TOP_SOUTH_STATION);
                  tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
                  break;
                case DOWN_AFTER:
                  release(Track.STONE_TRACK);
                  break;
              }
            } else if (x == 6 && y == 13) {
              switch (sensorCase) {
                case UP_BEFORE:
                  acquire(Track.STONE_TRACK);
                  release(Track.BOTTOM_SOUTH_STATION);
                  tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
                  break;
                case DOWN_AFTER:
                  release(Track.STONE_TRACK);
                  break;
              }
            }
            // metal track
            else if (x == 9 && y == 10) {
              switch (sensorCase) {
                case UP_BEFORE:
                  release(Track.STONE_TRACK);
                  acquire(Track.RIVER_TRACK);
                  tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
                  release(Track.BOTTOM_METAL_TRACK);
                  break;
                case DOWN_BEFORE:
                  release(Track.RIVER_TRACK);
                  acquire(Track.STONE_TRACK);
                  tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
                  release(Track.BOTTOM_METAL_TRACK);
                  break;
              }
            } else if (x == 9 && y == 9) {
              switch (sensorCase) {
                case UP_BEFORE:
                  release(Track.STONE_TRACK);
                  acquire(Track.RIVER_TRACK);
                  tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
                  release(Track.TOP_METAL_TRACK);
                  break;
                case DOWN_BEFORE:
                  release(Track.RIVER_TRACK);
                  acquire(Track.STONE_TRACK);
                  tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
                  release(Track.TOP_METAL_TRACK);
                  break;
              }
            }
            // angles
            else if (x == 19 && y == 9) {
              switch (sensorCase) {
                case UP_BEFORE:
                  if (semaphores[Track.TOP_NORTH_STATION.ordinal()].tryAcquire()) {
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
                  } else {
                    semaphores[Track.BOTTOM_NORTH_STATION.ordinal()].acquire();
                    tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
                  }
                  break;
                case DOWN_BEFORE:
                  if (semaphores[Track.TOP_METAL_TRACK.ordinal()].tryAcquire()) {
                    tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
                  } else {
                    semaphores[Track.BOTTOM_METAL_TRACK.ordinal()].acquire();
                    tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
                  }
                  break;
              }
            } else if (x == 1 && y == 9) {
              switch (sensorCase) {
                case UP_BEFORE:
                  if (semaphores[Track.TOP_METAL_TRACK.ordinal()].tryAcquire()) {
                    tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
                  } else {
                    semaphores[Track.BOTTOM_METAL_TRACK.ordinal()].acquire();
                    tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
                  }
                  break;
                case DOWN_BEFORE:
                  if (semaphores[Track.TOP_SOUTH_STATION.ordinal()].tryAcquire()) {
                    tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
                  } else {
                    semaphores[Track.BOTTOM_SOUTH_STATION.ordinal()].acquire();
                    tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
                  }
                  break;
              }
            }
            // stations
            else if (x == 13 && (y == 5 || y == 3)) {
              switch (sensorCase) {
                case UP_BEFORE:
                  stopTrain();
                  break;
              }
            } else if (x == 13 && (y == 11 || y == 13)) {
              switch (sensorCase) {
                case DOWN_BEFORE:
                  stopTrain();
                  break;
              }
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    new Thread(new Train(1, speed1, Track.TOP_NORTH_STATION)).start();
    new Thread(new Train(2, speed2, Track.TOP_SOUTH_STATION)).start();
  }
}

package sphere;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import robocode.HitByBulletEvent;
import robocode.HitObjectEvent;
import robocode.HitObstacleEvent;
import robocode.HitWallEvent;
import robocode.MessageEvent;
import robocode.ScannedObjectEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import CTFApi.CaptureTheFlagApi;

public class RandomFlagSeeker extends CaptureTheFlagApi {

	/**
	 * Note that CaptureTheFlagApi inherits TeamRobot, so users can directly use
	 * functions of TeamRobot.
	 */

	String[] myteam;
	Map<String, Point2D> myTeamPositions = new HashMap<String, Point2D>();

	Point2D enemyFlag;
	Rectangle2D homeBase;

	boolean flagCaptured = false;

	Point2D destination;
	int skipGoTo = 20;

	private Random random = new Random();

	public void run() {

		/*
		 * registerMe() needs to be called for every robot. Used for
		 * initialization.
		 */
		registerMe();

		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		while (true) {
			UpdateBattlefieldState(getBattlefieldState());
			myteam = getTeammates();
			homeBase = getOwnBase();
			enemyFlag = getEnemyFlag();

			if (!flagCaptured)
				destination = enemyFlag;
			else
				destination = new Point2D.Double(homeBase.getCenterX(), homeBase.getCenterY());

			//goTo(destination);
			
			randomWalk();

			turnRadar();

			sendMyPositionToTeammates();
			
			// write your logic here

			execute();
		}
	}

	private void turnRadar() {
		if (getRadarTurnRemaining() == 0.0) {
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		}
	}

	public void goTo(Point2D point) {
		// Logic to go to a particular point.
		
		double angle = Math.PI / 2 - Math.atan2(point.getY() - getY(), point.getX() - getX());
		if(angle < 0) {
			angle += Math.PI * 2;
		}

		double d = angle - getHeadingRadians();
		if(d > 0) {
			turnRightRadians(d);
		} else {
			turnLeftRadians(-d);
		}
		setAhead(10);
	}

	private void randomWalk() {
		turnRight((random.nextDouble() - 0.5) * 90);
		setAhead(100);
	}

	public void onHitObject(HitObjectEvent event) {
		if (event.getType().equals("flag") && enemyFlag.distance(getX(), getY()) < 50) {
			flagCaptured = true;
			skipGoTo = 0;
		} else if (event.getType().equals("base") && homeBase.contains(new Point2D.Double(getX(), getY()))) {
			flagCaptured = false;
			skipGoTo = 0;
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		if (!isTeammate(e.getName())) {
			double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
			double enemyDistance = e.getDistance();
			double enemyVelocity = e.getVelocity();

			setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians()));
			setFire(1);

			// lock radar on target
			setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 1.9);
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		// Turn to confuse the other robot
		setTurnRight(5);
	}

	public void onHitObstacle(HitObstacleEvent e) {
		back(20);
		turnRight(30);
		ahead(30);
	}

	public void onHitWall(HitWallEvent e) {
		// Move away from the wall
		back(20);
		turnRight(30);
		ahead(30);
	}

	public void onScannedObject(ScannedObjectEvent e) {
		if (e.getObjectType().equals("flag")) {
			// flag found
		}
	}

	private void sendMyPositionToTeammates() {
		try {
			broadcastMessage(new PositionMessage(new Point2D.Double(getX(), getY())));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	@Override
	public void onMessageReceived(MessageEvent event) {
		Serializable message = event.getMessage();
		if(message instanceof PositionMessage) {
			PositionMessage positionMessage = (PositionMessage) message;
			myTeamPositions.put(event.getSender(), positionMessage.getPosition());
		}

		super.onMessageReceived(event);
	}
	
	public class PositionMessage implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private Point2D position;

		public PositionMessage(Point2D position) {
			this.position = position;
		}

		public Point2D getPosition() {
			return position;
		}

		public void setPosition(Point2D position) {
			this.position = position;
		}

	}
}

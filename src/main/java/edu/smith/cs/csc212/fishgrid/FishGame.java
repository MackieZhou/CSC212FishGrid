package edu.smith.cs.csc212.fishgrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class manages our model of gameplay: missing and found fish, etc.
 * 
 * @author jfoley
 *
 */
public class FishGame {
	/**
	 * This is the world in which the fish are missing. (It's mostly a List!).
	 */
	World world;
	/**
	 * The player (a Fish.COLORS[0]-colored fish) goes seeking their friends.
	 */
	Fish player;
	/**
	 * The home location.
	 */
	FishHome home;
	/**
	 * These are the missing fish!
	 */
	List<Fish> missing;

	/**
	 * These are fish we've found!
	 */
	List<Fish> found;

	/**
	 * these are fish we've brought home
	 */
	List<Fish> homeFish;

	/**
	 * Number of steps!
	 */
	int stepsTaken;

	/**
	 * Score!
	 */
	int score;

	/**
	 * At random intervals, hearts will appear somewhere on the board.
	 */
	int heartTime;

	/**
	 * the number of rock we will generate
	 */
	public static final int NUM_ROCKS = 25;

	/**
	 * the number of snail we will generate
	 */
	public static final int NUM_SNAILS = 2;

	/**
	 * Create a FishGame of a particular size.
	 * 
	 * @param w how wide is the grid?
	 * @param h how tall is the grid?
	 */
	public FishGame(int w, int h) {
		world = new World(w, h);

		missing = new ArrayList<Fish>();
		found = new ArrayList<Fish>();

		// Add a home!
		home = world.insertFishHome();
		homeFish = new ArrayList<Fish>();

		// when do the hearts appear?
		this.heartTime = 1;

		// Add rocks
		for (int i = 0; i < NUM_ROCKS; i++) {
			world.insertRockRandomly();
		}

		// Make the snail!
		for (int i = 0; i < NUM_SNAILS; i++) {
			world.insertSnailRandomly();
		}

		// Make the player out of the 0th fish color.
		player = new Fish(0, world);
		// Start the player at "home".
		player.setPosition(home.getX(), home.getY());
		player.markAsPlayer();
		world.register(player);

		// Generate fish of all the colors but the first into the "missing" List.
		for (int ft = 1; ft < Fish.COLORS.length; ft++) {
			Fish friend = world.insertFishRandomly(ft);
			missing.add(friend);
		}
	}

	/**
	 * @return the size of the missing list.
	 */
	public int missingFishLeft() {
		return missing.size();
	}

	/**
	 * This method is how the Main app tells whether we're done.
	 * 
	 * @return true if the player has won (or maybe lost?).
	 */
	public boolean gameOver() {
		return homeFish.size() == Fish.COLORS.length - 1;
	}

	/**
	 * Update positions of everything (the user has just pressed a button).
	 */
	public void step() {
		// Keep track of how long the game has run.
		this.stepsTaken += 1;

		// make the hearts appear at random intervals
		if (this.stepsTaken == heartTime) {
			// calculate the new interval/heartTime
			calcHeartTime();
			// here come the hearts!
			insertHeart();
		}

		// These are all the objects in the world in the same cell as the player.
		List<WorldObject> overlap = this.player.findSameCell();
		// The player is there, too, let's skip them.
		overlap.remove(this.player);

		// If we find a fish, remove it from missing.
		for (WorldObject wo : overlap) {
			// It is missing if it's in our missing list.
			if (missing.contains(wo)) {
				if (!(wo instanceof Fish)) {
					throw new AssertionError("wo must be a Fish since it was in missing!");
				}
				// (Cast our WorldObject to a Fish)
				// Convince Java it's a Fish (we know it is!)
				Fish justFound = (Fish) wo;

				// Add to found list.
				found.add(justFound);

				// Remove from missing list.
				missing.remove(justFound);

				// Increase score when you find a fish!
				if (justFound.color == 0 || justFound.color == 8) {
					// red and magenta fish worth 25 points! Wow!
					score += 25;
				} else {
					// fish of other colors worth only 10 points...
					score += 10;
				}

			} else if (wo.isHeart()) {
				// If we find a Heart, remove it from allHearts
				wo.remove();
				// Hearts found by the player worths 10 points
				score += 10;
			}
		}

		// Make sure missing fish *do* something.
		wanderMissingFish();

		// if the a missing fish that wanders home accidentally...
		List<WorldObject> thingsAtHome = this.home.findSameCell();
		// remove the player fish if it's in the list
		thingsAtHome.remove(this.player);
		for (WorldObject wo : thingsAtHome) {
			if (wo.isFish() && !(wo.isPlayer()) && this.missing.contains(wo)) {
				// Fish that wander home by accident is marked at home
				homeFish.add((Fish) wo);
				// remove the fish from the missing list as well as the world
				wo.remove();
				this.missing.remove(wo);
			}
		}

		// hearts found by a missing fish worth no point
		for (Fish f : this.missing) {
			List<WorldObject> underFish = f.findSameCell();
			for (WorldObject wo : underFish) {
				if (wo.isHeart()) {
					wo.remove();
				}
			}
		}

		// after following a certain number of steps, fish found by the player gets
		// tired
		int stepTillTired = 15;
		List<Fish> copyFound = this.found;
		for (int i = 0; i < copyFound.size(); i++) {
			copyFound.get(i).followStep++;
			if (i >= 1) {
				// only the fish whose index in the found list is bigger than 1 stops following
				lostAgain(copyFound.get(i), stepTillTired);
			}
		}

		// When the player is at home, remove followers
		if (this.player.inSameSpot(this.home)) {
			goHome();
		}
		// When fish get added to "found" they will follow the player around.
		World.objectsFollow(player, found);

		// Step any world-objects that run themselves.
		world.stepAll();
	}

	/**
	 * Call moveRandomly() on all of the missing fish to make them seem alive.
	 */
	private void wanderMissingFish() {
		Random rand = ThreadLocalRandom.current();
		for (Fish lost : missing) {
			if (lost.fastscared == true) {
				// 80% of the time, fastscared lost fish move randomly.
				if (rand.nextDouble() < 0.8) {
					lost.moveRandomly();
				}
			} else {
				// 30% of the time, non-fastscared lost fish move randomly.
				if (rand.nextDouble() < 0.3) {
					lost.moveRandomly();
				}
			}

		}
	}

	/**
	 * This gets a click on the grid. We want it to destroy rocks that ruin the
	 * game.
	 * 
	 * @param x - the x-tile.
	 * @param y - the y-tile.
	 */
	public void click(int x, int y) {
		// use this print to debug your World.canSwim changes!
		List<WorldObject> atPoint = world.find(x, y);
		for (int i = 0; i < atPoint.size(); i++) {
			if (atPoint.get(i) instanceof Rock) {
				atPoint.get(i).remove();
			}
		}
	}

	public void goHome() {
		// take all fish found by the player home
		homeFish.addAll(found);
		// remove the fish from the world
		for (Fish fish : found) {
			fish.remove();
		}
		// reset the found list
		found.removeAll(found);
	}

	// Sweet has following the player fish for so long!!
	// Sweet decides to stop following the player
	public void lostAgain(Fish Sweet, int steps) {
		if (Sweet.followStep > steps) {
			this.missing.add(Sweet);
			this.found.remove(Sweet);
			// stop following and reset followStep to 0
			Sweet.followStep = 0;

			// you loose points when a fish stops following
			if (Sweet.color == 0 || Sweet.color == 8) {
				// red and magenta fish worth 25 points! Wow!
				score -= 25;
			} else {
				// fish of other colors worth only 10 points...
				score -= 10;
			}
		}
	}

	/**
	 * calculate the random interval for generating hearts
	 */
	public void calcHeartTime() {
		Random rand = ThreadLocalRandom.current();
		int interval = rand.nextInt(15) + 15;
		this.heartTime += interval;
	}

	/**
	 * insert random number of hearts
	 */
	public void insertHeart() {
		Random rand = ThreadLocalRandom.current();
		int howMany = rand.nextInt(3) + 2;

		// Add hearts randomly
		for (int i = 0; i < howMany; i++) {
			Heart h = new Heart(this.world);
			world.insertRandomly(h);
		}
	}

}

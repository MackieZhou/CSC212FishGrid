package edu.smith.cs.csc212.fishgrid;

public class FallingRock extends Rock {

	public FallingRock(World world) {
		super(world);
		this.color = ROCK_COLORS[rand.nextInt(ROCK_COLORS.length)];
	}
	
	@Override
	public void step() {
		this.moveDown();
	}

}

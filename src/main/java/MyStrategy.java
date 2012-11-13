
import model.*;

public final class MyStrategy implements Strategy {
	private State state;

	public MyStrategy() {
		state = State.Init;
	}
	
	@Override
	public void move(Tank self, World world, Move move) {
	    MediumStrategy strategy = new MediumStrategy(self, world, move, state);
	    strategy.run();
	    state = strategy.getState();
	}

	@Override
	public TankType selectTank(int tankIndex, int teamSize) {
	    return MediumStrategy.getTankType();
	}
}

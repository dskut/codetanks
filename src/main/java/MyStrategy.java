
import model.*;

enum State {
	Init,
	ToCorner,
	InCorner,
	Walk,
	OneOnOne,
}

public final class MyStrategy implements Strategy {
	private State state;

	public MyStrategy() {
		state = State.Init;
	}
	
	@Override
	public void move(Tank self, World world, Move move) {
	    SingleStrategyImpl strategy = new SingleStrategyImpl(self, world, move, state);
	    strategy.run();
	    state = strategy.getState();
	}

	@Override
	public TankType selectTank(int tankIndex, int teamSize) {
	    return SingleStrategyImpl.getTankType();
	}
}


import model.*;

enum State {
	Init,
	ToCorner,
	InCorner,
	Walk,
	OneOnOne,
	TwoOnOne,
}

public final class MyStrategy implements Strategy {
	private State state;

	public MyStrategy() {
		state = State.Init;
	}
	
	@Override
	public void move(Tank self, World world, Move move) {
	    int teamSize = 6 / world.getPlayers().length;
	    if (teamSize == 1) {
    	    SingleStrategyImpl strategy = new SingleStrategyImpl(self, world, move, state);
    	    strategy.run();
    	    state = strategy.getState();
	    } else if (teamSize == 2) {
    	    DoubleStrategyImpl strategy = new DoubleStrategyImpl(self, world, move, state);
    	    strategy.run();
    	    state = strategy.getState();
	    }
	}

	@Override
	public TankType selectTank(int tankIndex, int teamSize) {
	    return SingleStrategyImpl.getTankType();
	}
}

import model.*;

enum State {
    Init("Init"), 
    ToCorner("ToCorner"), 
    InCorner("InCorner"), 
    Walk("Walk"), 
    OneOnOne("OneOnOne"), 
    TwoOnOne("TwoOneOne");
    
    private final String text;

    private State(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}

public final class MyStrategy implements Strategy {
    private State state;

    public MyStrategy() {
        state = State.Init;
    }

    @Override
    public void move(Tank self, World world, Move move) {
        int teammates = BaseStrategyImpl.getAliveTeammates(self, world);
        if (teammates == 0) {
            SingleStrategyImpl strategy = new SingleStrategyImpl(self, world, move, state);
            strategy.run();
            state = strategy.getState();
        } else if (teammates == 1) {
            DoubleStrategyImpl strategy = new DoubleStrategyImpl(self, world, move, state);
            strategy.run();
            state = strategy.getState();
        } else {
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

from src.agent.rl.environment import MicroserviceOptimizerEnv
from src.agent.rl.reward_strategy import RewardStrategyFactory
from src.agent.rl.observation_strategy import ObservationStrategyFactory
from src.agent.simulator_evaluator import SimEvaluator

NODES = 3
MICROSERVICES = 8
WORKLOADS = [
    {
        "file": "workload.py",
        "users": 5,
        "spawn_rate": 5,
        # "run_time": "1m",
        "iterations": 5
    }
    # Add more workloads here if needed
]


def run_agent(
    trace_manager,
    reward_type="balance_delay_queue",
    observation_type="basic"
):
    """
    Starts the agent training loop.
    """

    reward_strat = RewardStrategyFactory.create(
        reward_type, alpha=0.5, beta=0.5)
    observation_strat = ObservationStrategyFactory.create(
        observation_type, MICROSERVICES, NODES)
    evaluator = SimEvaluator(trace_manager, WORKLOADS)

    # TODO: Decide algorithm
    print("Training Loop Started!")

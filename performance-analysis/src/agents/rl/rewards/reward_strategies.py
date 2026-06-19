from abc import ABC, abstractmethod


class RewardStrategy(ABC):
    @property
    @abstractmethod
    def stop_reward(self) -> float:
        """Reward value for the stop action (end early)."""
        pass

    @property
    @abstractmethod
    def invalid_action_reward(self) -> float:
        """Reward value when an agent executes an invalid action."""
        pass

    @property
    @abstractmethod
    def time_tax(self) -> float:
        """Tax to subtract from the reward overtime to ensure the agent is as quick as possible."""
        pass

    @abstractmethod
    def compute(self, old_metrics: dict, new_metrics: dict) -> float:
        """Computes the reward based on current metrics."""
        pass


class RewardStrategyFactory:
    @staticmethod
    def create(strategy_type: str, **kwargs) -> RewardStrategy:
        if strategy_type == "balance_delay_queue":
            return BalanceDelayQueueReward(**kwargs)
        elif strategy_type == "global_average_latency":
            return GlobalAverageLatencyReward(**kwargs)
        else:
            raise ValueError(f"Unknown reward strategy: {strategy_type}")


class BalanceDelayQueueReward(RewardStrategy):
    """Treats every service as equally important, ensure none of them becomes a bottleneck"""

    stop_reward = -0.1
    invalid_action_reward = -1
    time_tax = 0.05

    def __init__(self, alpha=1.0, beta=1.0):
        self.alpha = alpha  # Weight for Queue Time
        self.beta = beta    # Weight for Network Delay

    def compute(self, old_metrics, new_metrics) -> float:
        if old_metrics is None or not old_metrics.get("microservices"):
            return 0.0

        if new_metrics is None or not new_metrics.get("microservices"):
            return -1.0  # Failed evaluation

        old_mss_metrics = old_metrics.get("microservices", {})
        new_mss_metrics = new_metrics.get("microservices", {})

        def get_avg_metric(m, key):
            invocs = m.get("invocations", 0)
            return m.get(key, 0) / invocs if invocs > 0 else 0.0

        old_q_time = sum(get_avg_metric(m, "queue_time")
                         for m in old_mss_metrics.values())
        old_d_time = sum(get_avg_metric(m, "delay_time")
                         for m in old_mss_metrics.values())

        new_q_time = sum(get_avg_metric(m, "queue_time")
                         for m in new_mss_metrics.values())
        new_d_time = sum(get_avg_metric(m, "delay_time")
                         for m in new_mss_metrics.values())

        delta_q = old_q_time - new_q_time
        delta_d = old_d_time - new_d_time

        return (self.alpha * delta_q) + (self.beta * delta_d)


class GlobalAverageLatencyReward(RewardStrategy):
    """
    Naturally biased towards high-traffic microservices, optimizing for user experience.
    """

    stop_reward = -0.1
    invalid_action_reward = -1
    time_tax = 0.05

    def __init__(self, alpha=1.0, beta=1.0):
        self.alpha = alpha
        self.beta = beta

    def compute(self, old_metrics, new_metrics) -> float:
        if old_metrics is None or not old_metrics.get("microservices"):
            return 0.0

        if new_metrics is None or not new_metrics.get("microservices"):
            return -1.0

        old_mss = old_metrics.get("microservices", {}).values()
        new_mss = new_metrics.get("microservices", {}).values()

        def get_global_avg(mss, key):
            total_time = sum(m.get(key, 0) for m in mss)
            total_invocs = sum(m.get("invocations", 0) for m in mss)
            return total_time / total_invocs if total_invocs > 0 else 0.0

        old_q_avg = get_global_avg(old_mss, "queue_time")
        old_d_avg = get_global_avg(old_mss, "delay_time")

        new_q_avg = get_global_avg(new_mss, "queue_time")
        new_d_avg = get_global_avg(new_mss, "delay_time")

        delta_q = old_q_avg - new_q_avg
        delta_d = old_d_avg - new_d_avg

        return (self.alpha * delta_q) + (self.beta * delta_d)

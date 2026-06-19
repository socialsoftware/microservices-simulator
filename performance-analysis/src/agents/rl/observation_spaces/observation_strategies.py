from abc import ABC, abstractmethod
import numpy as np
from gymnasium import spaces
from src.simulator_tools.config_utils import ConfigTool
from src.agents.simulation_runner import WorkloadConfig


class ObservationStrategy(ABC):
    def __init__(self, num_services: int, num_nodes: int):
        self.num_services = num_services
        self.num_nodes = num_nodes

    @abstractmethod
    def get_space(self) -> spaces.Space:
        """Defines the shape of the space."""
        pass

    @abstractmethod
    def build_observation(self, active_config: dict, workload_profile: WorkloadConfig) -> dict:
        """Translates the enviroment state into the correct observation space format."""
        pass


class ObservationStrategyFactory:
    @staticmethod
    def create(strategy_type: str, num_services: int, num_nodes: int, **kwargs) -> ObservationStrategy:
        if strategy_type == "basic":
            return BasicObservation(num_services, num_nodes, **kwargs)
        else:
            raise ValueError(f"Unknown observation strategy: {strategy_type}")


class BasicObservation(ObservationStrategy):
    def get_space(self):
        # We do not need to include node capacity because they are static
        return spaces.Dict({
            "placement": spaces.MultiDiscrete([self.num_nodes] * self.num_services),
            "capacities": spaces.Box(low=1, high=100, shape=(self.num_services,), dtype=np.float32),
            "workload_profile": spaces.Box(low=0, high=np.inf, shape=(3,), dtype=np.float32)
        })

    def build_observation(self, active_config, workload_profile):
        microservices = ConfigTool.get_microservices_list(active_config)

        placement_map = ConfigTool.get_ms_placement_map(active_config)
        placement_array = [placement_map.get(ms, 0) for ms in microservices]

        capacity_map = ConfigTool.get_all_ms_capacities(active_config)
        capacities_array = [capacity_map.get(ms, 1) for ms in microservices]

        return {
            "placement": np.array(placement_array, dtype=np.int64),
            "capacities": np.array(capacities_array, dtype=np.float32),
            "workload_profile": np.array([workload_profile.users, workload_profile.iterations, workload_profile.rw_ratio], dtype=np.float32)
        }

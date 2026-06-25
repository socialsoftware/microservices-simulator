from abc import ABC, abstractmethod
import numpy as np
from gymnasium import spaces
from src.simulator_tools.config_utils import ConfigTool
from src.agents.simulation_runner import WorkloadConfig


class ObservationStrategy(ABC):
    def __init__(self, num_services: int, num_nodes: int, **kwargs):
        self.num_services = num_services
        self.num_nodes = num_nodes

    @abstractmethod
    def get_space(self) -> spaces.Space:
        """Defines the shape of the space."""
        pass

    @abstractmethod
    def build_observation(self, active_config: dict, metrics: dict) -> dict:
        """Translates the enviroment state into the correct observation space format."""
        pass


class ObservationStrategyFactory:
    @staticmethod
    def create(strategy_type: str, num_services: int, num_nodes: int, **kwargs) -> ObservationStrategy:
        if strategy_type == "basic":
            return BasicObservation(num_services, num_nodes, **kwargs)
        elif strategy_type == "complex":
            return ComplexObservation(num_services, num_nodes, **kwargs)
        else:
            raise ValueError(f"Unknown observation strategy: {strategy_type}")


class BasicObservation(ObservationStrategy):
    def get_space(self):
        return spaces.Dict({
            "placement": spaces.MultiDiscrete([self.num_nodes] * self.num_services),
            "capacities": spaces.Box(low=1, high=100, shape=(self.num_services,), dtype=np.float32),
        })

    def build_observation(self, active_config, metrics):
        # This observation type ignores metrics
        microservices = ConfigTool.get_microservices_list(active_config)

        placement_map = ConfigTool.get_ms_placement_map(active_config)
        placement_array = [placement_map.get(ms, 0) for ms in microservices]

        capacity_map = ConfigTool.get_all_ms_capacities(active_config)
        capacities_array = [capacity_map.get(ms, 1) for ms in microservices]

        return {
            "placement": np.array(placement_array, dtype=np.int64),
            "capacities": np.array(capacities_array, dtype=np.float32),
        }


class ComplexObservation(ObservationStrategy):
    def __init__(self, num_services: int, num_nodes: int, **kwargs):
        super().__init__(num_services, num_nodes, **kwargs)
        if not "run_time" in kwargs:
            raise ValueError("Complex observation must include run-time!")

        self.run_time = kwargs["run_time"]
        # TODO: Think about this
        # self.ms_order = ms_order

    def get_space(self):
        # We do not need to include node capacity because they are static
        return spaces.Dict({
            "placement": spaces.MultiDiscrete([self.num_nodes] * self.num_services),
            "capacities": spaces.Box(low=1, high=100, shape=(self.num_services,), dtype=np.float32),
            "node_free_caps": spaces.Box(low=0, high=np.inf, shape=(self.num_nodes,), dtype=np.float32),

            # "ms_rps": spaces.Box(low=0, high=np.inf, shape=(self.num_services,), dtype=np.float32),
            "ms_delay": spaces.Box(low=0, high=np.inf, shape=(self.num_services,), dtype=np.float32),
            "ms_queue": spaces.Box(low=0, high=np.inf, shape=(self.num_services,), dtype=np.float32)
        })

    def build_observation(self, active_config, metrics):
        """
        Converts the JSON configuration and OpenTelemetry JSON metrics into raw Numpy arrays.
        """
        # TODO: Test
        placement_map = ConfigTool.get_ms_placement_map(active_config)
        capacity_map = ConfigTool.get_all_ms_capacities(active_config)
        node_caps = ConfigTool.get_node_capacities(active_config)
        microservices = ConfigTool.get_microservices_list(active_config)

        placement_array = []
        capacities_array = []

        # Loop using microservice list to guarantee index consistency across all arrays
        for microservice in microservices:
            placement_array.append(placement_map.get(microservice, 0))
            capacities_array.append(capacity_map.get(microservice, 1))

        free_caps_array = []
        for i in range(self.num_nodes):
            limit = node_caps[i]["limit"]
            used = node_caps[i]["used"]
            free_caps_array.append(limit - used)

        rps_array = []
        delay_array = []
        queue_array = []

        ms_metrics = metrics.get("microservices", {})

        for microservice in microservices:
            data = ms_metrics.get(microservice, {
                "invocations": 0,
                "delay_time": 0.0,
                "queue_time": 0.0
            })

            invocations = data.get("invocations")

            if self.run_time > 0:
                rps = invocations / self.run_time
            else:
                rps = 0.0

            if invocations > 0:
                # TODO: Use P50 (median)
                avg_delay = data.get("delay_time", 0.0) / invocations
                avg_queue = data.get("queue_time", 0.0) / invocations
            else:
                avg_delay = 0.0
                avg_queue = 0.0

            rps_array.append(rps)
            delay_array.append(avg_delay)
            queue_array.append(avg_queue)

        return {
            "placement": np.array(placement_array, dtype=np.int64),
            "capacities": np.array(capacities_array, dtype=np.float32),
            "node_free_caps": np.array(free_caps_array, dtype=np.float32),
            # "ms_rps": np.array(rps_array, dtype=np.float32),
            "ms_delay": np.array(delay_array, dtype=np.float32),
            "ms_queue": np.array(queue_array, dtype=np.float32)
        }

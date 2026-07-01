import gymnasium as gym
from gymnasium import spaces
import numpy as np
import copy
import random
import logging
from src.agents.rl.observation_spaces.observation_strategies import ObservationStrategy
from src.agents.rl.rewards.reward_strategies import RewardStrategy
from src.agents.simulation_runner import SimRunner, WorkloadConfig
from src.agents.rl.action_spaces.actions import Action, get_action_mapping, get_valid_action_mask, AMOUNT
from src.simulator_tools.config_utils import ConfigTool
from src.simulator_tools.simulator_utils import SimInterface


class MicroserviceOptimizerEnv(gym.Env):
    def __init__(
        self,
        sim_runner: SimRunner,
        workloads: list[WorkloadConfig],
        users_interval: tuple[int, int],
        iterations_interval: tuple[int, int],
        weights_interval: tuple[float, float],
        wait_time: tuple[float, float],
        run_time: int,
        reward_strat: RewardStrategy,
        observation_strat: ObservationStrategy,
        num_services: int,
        num_nodes: int,
        max_steps: int
    ):

        super(MicroserviceOptimizerEnv, self).__init__()

        self.sim_runner = sim_runner
        self.workloads = workloads
        self.users_itvl = users_interval
        self.iterations_itvl = iterations_interval
        self.weights_itvl = weights_interval
        self.wait_time_itvl = wait_time
        self.run_time = run_time
        self.reward_strategy = reward_strat
        self.obs_strategy = observation_strat

        self.num_nodes = num_nodes

        self.action_mapping = get_action_mapping(
            num_services, num_nodes)

        self.microservices = ConfigTool.get_microservices_list(
            sim_runner.base_config)

        self.action_space = spaces.Discrete(len(self.action_mapping))
        self.observation_space = self.obs_strategy.get_space()

        self.wl_config = None
        self.last_metrics = None
        self.current_step = 0
        self.max_steps = max_steps

    def valid_action_mask(self):
        """Returns the action masking function."""
        return get_valid_action_mask(self.action_mapping, self.sim_runner.current_config, self.microservices)

    def _randomize_workload(self) -> WorkloadConfig:
        """Returns a randomized workload configuration."""

        file = random.choice(self.workloads)
        users = random.randint(*self.users_itvl)
        # Locust does not work well above a ramp-up value of 100, so we cap it there
        spawn_rate = users if users <= 100 else 100
        iterations = random.randint(*self.iterations_itvl)
        read_weight = random.uniform(*self.weights_itvl)
        write_weight = random.uniform(*self.weights_itvl)
        wait_time = random.uniform(*self.wait_time_itvl)

        logging.info(
            f"Workload: file={file}, users={users}, iterations={iterations}, read_w={read_weight:.2f}, write_w={write_weight:.2f}, wait_t={wait_time:.2f}")
        return WorkloadConfig(file, users, spawn_rate, iterations, self.run_time, read_weight, write_weight, wait_time)

    def reset(self, seed=None, options=None):
        """
        @Overide:
        Resets the environment randomly generating a new configuration and workload.
        Returns the initial observation state.
        """

        super().reset(seed=seed)
        self.current_step = 0

        self.wl_config = self._randomize_workload()

        new_config = ConfigTool.randomize_config(
            self.sim_runner.base_config, self.microservices, self.num_nodes)

        SimInterface.stop()
        metrics = self.sim_runner.evaluate_configuration(
            new_config, self.wl_config)
        self.last_metrics = metrics

        initial_observation = self.obs_strategy.build_observation(
            new_config, metrics)

        return initial_observation, {}

    def step(self, action):
        """
        @Overide:
        Takes a valid step in the environment generating a new observation and reward.
        """

        new_config, is_illegal_action, is_stop_action = self._act(
            action, self.sim_runner.current_config)

        obs, reward = self._observe(
            new_config, is_illegal_action, is_stop_action)

        self.current_step += 1
        terminated = is_stop_action
        truncated = self.current_step >= self.max_steps

        logging.info(
            f"Step ended with reward: {reward} and obs: {obs}")
        return obs, reward, terminated, truncated, {}

    def _act(self, action_idx: int, current_config: dict) -> tuple[dict, bool, bool]:
        """
        Parses the action index and attempts to mutate the configuration.
        Returns a tuple: (new_config, illegal_action, stop_action)
        """

        action_tuple = self.action_mapping[action_idx]
        action_type = action_tuple[0]

        config_to_update = copy.deepcopy(current_config)
        is_illegal_action = False
        is_stop_action = False

        if action_type == Action.Migrate:
            microservice_id = action_tuple[1]
            target_node_id = action_tuple[2]
            microservice_name = self.microservices[microservice_id]

            if not ConfigTool.migrate_microservice(
                    config_to_update, microservice_name, target_node_id):
                is_illegal_action = True

        elif action_type == Action.ScaleUp:
            microservice_id = action_tuple[1]
            multiplier = action_tuple[2]
            total_amount = AMOUNT * multiplier
            microservice_name = self.microservices[microservice_id]

            if not ConfigTool.scale_up_capacity(
                    config_to_update, microservice_name, total_amount):
                is_illegal_action = True

        elif action_type == Action.ScaleDown:
            microservice_id = action_tuple[1]
            multiplier = action_tuple[2]
            total_amount = AMOUNT * multiplier
            microservice_name = self.microservices[microservice_id]

            if not ConfigTool.scale_down_capacity(
                    config_to_update, microservice_name, total_amount):
                is_illegal_action = True

        else:
            is_stop_action = True

        logging.info(
            f"Action selected: {action_tuple}, illegal: {is_illegal_action}, stop_op: {is_stop_action}")
        return config_to_update, is_illegal_action, is_stop_action

    def _observe(self, new_config, is_illegal_action, is_stop_action):
        """
        Evaluates the config, computes the reward, and builds the next observation.
        """

        if is_illegal_action:
            # Fallback in case action mask is not applied correctly
            reward = self.reward_strategy.invalid_action_reward
            metrics = self.last_metrics
            obs = self.obs_strategy.build_observation(
                self.sim_runner.current_config, metrics)
        elif is_stop_action:
            reward = self.reward_strategy.stop_reward
            metrics = self.last_metrics
            obs = self.obs_strategy.build_observation(
                self.sim_runner.current_config, metrics)
        else:
            metrics = self.sim_runner.evaluate_configuration(
                new_config, self.wl_config)
            reward = self.reward_strategy.compute(self.last_metrics, metrics)
            self.last_metrics = metrics
            obs = self.obs_strategy.build_observation(
                self.sim_runner.current_config, metrics)

        reward -= self.reward_strategy.time_tax

        return obs, reward

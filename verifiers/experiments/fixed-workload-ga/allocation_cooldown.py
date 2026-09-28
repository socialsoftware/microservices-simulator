"""One-other-attempt missing-feedback guard for adaptive allocators."""

from allocator import ContextualLinUcbPolicy


POLICY = 'contextual-linucb-cooldown'


class MissingFeedbackCooldownPolicy:
    """Apply the same deterministic null-feedback guard to any allocator model."""

    def __init__(self, policy):
        self.policy = policy
        self.cooldown = None
        self.cooldown_count = 0

    def select(self, active, states):
        eligible = set(active)
        excluded = None
        if self.cooldown in eligible and len(eligible) > 1:
            excluded = self.cooldown
            eligible.remove(self.cooldown)
        workload_id, metadata, context = self.policy.select(eligible, states)
        return workload_id, {**metadata, 'missingFeedbackCooldownExcluded': excluded}, context

    def update(self, workload_id, context, reward):
        updated = self.policy.update(workload_id, context, reward)
        if reward is None:
            self.cooldown = workload_id
            self.cooldown_count += 1
        else:
            self.cooldown = None
        return updated

    def summary(self):
        return {**self.policy.summary(), 'missingFeedbackCooldown': 'one-other-attempt',
                'cooldownEvents': self.cooldown_count,
                'cooldownWorkloadAtStop': self.cooldown}

    def __getattr__(self, name):
        return getattr(self.policy, name)


class ContextualLinUcbCooldownPolicy(MissingFeedbackCooldownPolicy):
    """Compatibility constructor for the original guarded contextual policy."""

    def __init__(self, feature_space, exploration, ridge):
        super().__init__(ContextualLinUcbPolicy(feature_space, exploration, ridge))


def make_policy(parameters, feature_space):
    """Build the explicit variant using the base policy's parameters unchanged."""
    return ContextualLinUcbCooldownPolicy(
        feature_space, parameters['exploration'], parameters['ridge'])

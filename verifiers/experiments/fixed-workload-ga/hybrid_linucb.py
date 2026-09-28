"""Injectable hybrid LinUCB for bounded cross-workload experiments.

This implements the shared/local model from Li et al., "A Contextual-Bandit
Approach to Personalized News Article Recommendation", WWW 2010.  Shared
structural coefficients coexist with independent per-workload progress
coefficients.  The recursive update is the Schur-complement form of the direct
ridge block system.
"""
import math

from allocator import FeatureSpace, PROGRESS_FEATURES


PAPER = 'https://archives.iw3c2.org/www2010/_lihong/pub/Li10Contextual.pdf'


def _number(value, *, positive=False, nonnegative=False, label='value'):
    valid = type(value) in (int, float) and math.isfinite(value)
    if positive:
        valid = valid and value > 0
    if nonnegative:
        valid = valid and value >= 0
    if not valid:
        qualifier = 'positive' if positive else 'nonnegative'
        raise ValueError(f'{label} must be a finite {qualifier} number')
    return float(value)


def _dot(left, right):
    if len(left) != len(right):
        raise ValueError('Vector dimensions differ')
    return math.fsum(a * b for a, b in zip(left, right))


def _matvec(matrix, vector):
    return [_dot(row, vector) for row in matrix]


def _transpose_matvec(matrix, vector):
    if len(matrix) != len(vector):
        raise ValueError('Matrix/vector dimensions differ')
    if not matrix:
        return []
    return [math.fsum(matrix[row][column] * vector[row]
                      for row in range(len(matrix)))
            for column in range(len(matrix[0]))]


def _identity_inverse(dimension, ridge):
    return [[1.0 / ridge if row == column else 0.0
             for column in range(dimension)] for row in range(dimension)]


def _finite_vector(vector, dimension, label):
    if not isinstance(vector, (list, tuple)) or len(vector) != dimension \
            or any(type(value) not in (int, float) or not math.isfinite(value)
                   for value in vector):
        raise ValueError(f'{label} returned an invalid feature vector')
    return [float(value) for value in vector]


class StructuralFeatureSpace:
    """The existing structural vocabulary with observed progress removed."""

    def __init__(self, profiles):
        self.full = FeatureSpace(profiles)
        self.positions = [index for index, name in enumerate(self.full.names)
                          if name not in PROGRESS_FEATURES]
        self.names = [self.full.names[index] for index in self.positions]

    def vector(self, profile, progress):
        full = self.full.vector(profile, progress)
        return [full[index] for index in self.positions]


class _LocalState:
    def __init__(self, local_dimension, shared_dimension, ridge):
        self.inverse = _identity_inverse(local_dimension, ridge)
        self.cross = [[0.0] * shared_dimension for _ in range(local_dimension)]
        self.response = [0.0] * local_dimension
        self.updates = 0


class HybridLinUcbPolicy:
    """Hybrid LinUCB with shared structure and per-workload progress models."""

    def __init__(self, shared_feature_space, local_feature_space, workload_ids,
                 exploration=1.0, ridge=1.0):
        self.shared_feature_space = shared_feature_space
        self.local_feature_space = local_feature_space
        self.exploration = _number(exploration, nonnegative=True, label='exploration')
        self.ridge = _number(ridge, positive=True, label='ridge')
        self.shared_dimension = len(shared_feature_space.names)
        self.local_dimension = len(local_feature_space.names)
        if not self.shared_dimension or not self.local_dimension:
            raise ValueError('Hybrid feature spaces must be nonempty')
        identities = list(workload_ids)
        if not identities or len(identities) != len(set(identities)) \
                or any(not isinstance(value, str) or not value for value in identities):
            raise ValueError('Workload identities must be unique nonempty strings')
        self.workload_ids = tuple(sorted(identities))
        self.shared_inverse = _identity_inverse(self.shared_dimension, self.ridge)
        self.shared_response = [0.0] * self.shared_dimension
        self.shared_theta = [0.0] * self.shared_dimension
        self.locals = {workload_id: _LocalState(self.local_dimension,
                                                self.shared_dimension, self.ridge)
                       for workload_id in self.workload_ids}
        self.updates = 0

    def context(self, workload_id, state):
        if workload_id not in self.locals:
            raise ValueError('Unknown hybrid workload identity')
        if not isinstance(state, dict) or 'profile' not in state or 'progress' not in state:
            raise ValueError('Hybrid state requires profile and progress')
        shared = _finite_vector(
            self.shared_feature_space.vector(state['profile'], state['progress']),
            self.shared_dimension, 'Shared feature space')
        local = _finite_vector(
            self.local_feature_space.vector(state['profile'], state['progress']),
            self.local_dimension, 'Local feature space')
        return {'workload': workload_id, 'shared': shared, 'local': local}

    def _score(self, workload_id, context):
        local_state = self.locals[workload_id]
        z, x = context['shared'], context['local']
        local_projection = _matvec(local_state.inverse, x)
        residual_shared = [value - correction for value, correction in
                           zip(z, _transpose_matvec(local_state.cross, local_projection))]
        local_mean = _dot(local_projection, local_state.response)
        estimate = local_mean + _dot(residual_shared, self.shared_theta)
        shared_projection = _matvec(self.shared_inverse, residual_shared)
        variance = _dot(x, local_projection) + _dot(residual_shared, shared_projection)
        uncertainty = self.exploration * math.sqrt(max(0.0, variance))
        return estimate + uncertainty, estimate, uncertainty

    def select(self, active, states):
        active = set(active)
        if not active or not active <= set(self.workload_ids):
            raise ValueError('Active hybrid workloads must be a nonempty known subset')
        if not active <= set(states):
            raise ValueError('Hybrid state is missing an active workload')
        best = None
        for workload_id in sorted(active):
            context = self.context(workload_id, states[workload_id])
            index, estimate, uncertainty = self._score(workload_id, context)
            row = (index, workload_id, estimate, uncertainty, context)
            # Sorted iteration plus a strict comparison makes cold-start ties stable.
            if best is None or index > best[0]:
                best = row
        index, workload_id, estimate, uncertainty, context = best
        return workload_id, {'index': index, 'estimate': estimate,
                             'uncertainty': uncertainty}, context

    def _validate_context(self, workload_id, context):
        if not isinstance(context, dict) or set(context) != {'workload', 'shared', 'local'} \
                or context.get('workload') != workload_id:
            raise ValueError('Hybrid update context/workload identity mismatch')
        return (_finite_vector(context['shared'], self.shared_dimension, 'Shared context'),
                _finite_vector(context['local'], self.local_dimension, 'Local context'))

    def update(self, workload_id, context, reward):
        if workload_id not in self.locals:
            raise ValueError('Unknown hybrid workload identity')
        z, x = self._validate_context(workload_id, context)
        if reward is None:
            return False
        reward = _number(reward, nonnegative=True, label='reward')
        local = self.locals[workload_id]

        # Conditional (Schur-complement) recursive least-squares update.  This is
        # algebraically equal to the two A0 updates in Algorithm 2 of the paper.
        local_projection = _matvec(local.inverse, x)
        local_denominator = 1.0 + _dot(x, local_projection)
        residual_shared = [value - correction for value, correction in
                           zip(z, _transpose_matvec(local.cross, local_projection))]
        local_prediction = _dot(local_projection, local.response)

        shared_projection = _matvec(self.shared_inverse, residual_shared)
        shared_denominator = local_denominator + _dot(residual_shared, shared_projection)
        self.shared_inverse = [
            [self.shared_inverse[row][column]
             - shared_projection[row] * shared_projection[column] / shared_denominator
             for column in range(self.shared_dimension)]
            for row in range(self.shared_dimension)]
        response_scale = (reward - local_prediction) / local_denominator
        self.shared_response = [value + response_scale * coordinate
                                for value, coordinate in
                                zip(self.shared_response, residual_shared)]

        self.locals[workload_id].inverse = [
            [local.inverse[row][column]
             - local_projection[row] * local_projection[column] / local_denominator
             for column in range(self.local_dimension)]
            for row in range(self.local_dimension)]
        self.locals[workload_id].cross = [
            [local.cross[row][column] + x[row] * z[column]
             for column in range(self.shared_dimension)]
            for row in range(self.local_dimension)]
        self.locals[workload_id].response = [value + reward * coordinate
                                             for value, coordinate in
                                             zip(local.response, x)]
        self.locals[workload_id].updates += 1
        self.shared_theta = _matvec(self.shared_inverse, self.shared_response)
        self.updates += 1
        return True

    def coefficients(self, workload_id):
        """Return fitted shared/local coefficients for validation and diagnostics."""
        if workload_id not in self.locals:
            raise ValueError('Unknown hybrid workload identity')
        local = self.locals[workload_id]
        correction = _matvec(local.cross, self.shared_theta)
        theta = _matvec(local.inverse, [value - adjustment for value, adjustment in
                                        zip(local.response, correction)])
        return {'shared': list(self.shared_theta), 'local': theta}

    def summary(self):
        return {'modelUpdates': self.updates, 'exploration': self.exploration,
                'ridge': self.ridge, 'sharedFeatureCount': self.shared_dimension,
                'localFeatureCount': self.local_dimension, 'sharedParameters': True,
                'workloadSpecificParameters': True,
                'localUpdates': {workload_id: self.locals[workload_id].updates
                                 for workload_id in self.workload_ids},
                'algorithmReference': PAPER}

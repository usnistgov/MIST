from dataclasses import dataclass
import numpy as np
import copy
import enum


# local imports


class MleHillClimbDirection(enum.Enum):
    PosP = (1, 0, 0)
    NegP = (-1, 0, 0)
    PosM = (0, 1, 0)
    NegM = (0, -1, 0)
    PosS = (0, 0, 1)
    NegS = (0, 0, -1)
    NoMove = (0, 0, 0)

    def __init__(self, p: int, m: int, s: int):
        self.p = p
        self.m = m
        self.s = s

@dataclass
class MlePoint:
    pi_uni: int = 0.0
    mu: int = 0.0
    sigma: int = 0.0
    likelihood: float = -np.inf

    @staticmethod
    def getRandomPoint():
        p = np.random.randint(0, 100)
        m = np.random.randint(1, 100)
        s = np.random.randint(1, 100)
        return MlePoint(pi_uni=p, mu=m, sigma=s, likelihood=-np.inf)

    def update(self, dir: MleHillClimbDirection):
        self.pi_uni += dir.p
        self.mu += dir.m
        self.sigma += dir.s

    def is_valid(self):
        return self.pi_uni >= 0 and self.pi_uni < 100 and self.mu > 0 and self.mu < 100 and self.sigma > 0 and self.sigma < 100

    def __eq__(self, other):
        return self.pi_uni == other.pi_uni and self.mu == other.mu and self.sigma == other.sigma


class MleLikelihoodCache():
    def __init__(self):
        # cache to hold the mle likelihoods for valid integer percentages
        # init with all nan values
        self.cache = np.nan * np.ones((100,100,100), dtype=np.float32)

    # TODO might be better to use ints here
    def get(self, point: MlePoint) -> float:
        # convert to integers
        p = point.pi_uni
        m = point.mu
        s = point.sigma

        val = np.nan
        if p >= 0 and p < 100 and m > 0 and m < 100 and s > 0 and s < 100:
            val = self.cache[p, m, s]

        return val

    def set(self, point: MlePoint, val: float):
        # convert to integers
        p = point.pi_uni
        m = point.mu
        s = point.sigma

        if p >= 0 and p < 100 and m > 0 and m < 100 and s > 0 and s < 100:
            self.cache[p, m, s] = val


def computeLikelihood(point: MlePoint, translations: np.ndarray) -> float:
    """
    Method to compute the MLE model likelihood given the model parameters and a set of translations. The model parameters are specified as percentage value of the translations range.
    :param point: the mle control model to compute the likelihood of.
    :param translations: numpy array of translations to fit the model to. Must be within [0,100].
    :return: the likelihood of the model.
    """
    w_or_h = 100.0  # we have already normalized the translations into [0,100]
    translations = translations.ravel()
    likelihood = -np.inf
    if point.pi_uni >= 0 and point.pi_uni < 100:
        p = point.pi_uni / 100.0  # convert [0,100] to [0.0, 1.0] so its a percentage

        ts = np.copy(translations)  # don't modify translations
        ts = (ts - point.mu) / point.sigma
        ts = np.exp(-0.5 * ts * ts)
        ts = ts / (np.sqrt(2.0 * np.pi) * point.sigma)
        ts = (p / w_or_h) + (1.0 - p) * ts
        ts = np.log(np.abs(ts))
        likelihood = np.sum(ts)
    return likelihood


def hillClimbSearch(point: MlePoint, cache: MleLikelihoodCache, translations: np.ndarray) -> MlePoint:
    """
    Computes the MLE model using a percentile resolution hill climbing in the parameter space.
    :param point: the hill climbing starting point.
    :param cache: cache object for storing computed likelihood values.
    :param translations: list of translations to fit the model to. Must be within [0,100].
    :return: the MLE model with the highest likelihood found by the hill climbing.
    """

    lcl = copy.deepcopy(point)
    while True:
        # Check each direction and move based on highest correlation
        for d in MleHillClimbDirection._member_names_:
            dir = MleHillClimbDirection[d]
            if dir == MleHillClimbDirection.NoMove:
                continue

            # get the new mle point given the 3d hill climb direction
            new_point = copy.deepcopy(point)
            new_point.update(dir)
            if not new_point.is_valid():
                continue

            # check the cache to see if this likelihood has already been computed
            new_point.likelihood = cache.get(new_point)
            if np.isnan(new_point.likelihood):
                # compute the likelihood
                new_point.likelihood = computeLikelihood(new_point, translations)
                cache.set(new_point, new_point.likelihood)

            if new_point.likelihood > lcl.likelihood:
                lcl = new_point
        if lcl.likelihood > point.likelihood:
            # if the best local neighborhood point is better that the current global best
            point = lcl
        else:
            # the current point is the best in the local neighborhood, so hill climb has terminated
            break
    return point






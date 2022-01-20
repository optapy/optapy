from optapy import constraint_provider, get_class
from optapy.score import HardSoftScore
from optapy.constraint import ConstraintFactory
from domain import Vehicle

VehicleClass = get_class(Vehicle)


@constraint_provider
def vehicle_routing_constraints(constraint_factory: ConstraintFactory):
    return [
        vehicle_capacity(constraint_factory),
        total_distance(constraint_factory)
    ]


def vehicle_capacity(constraint_factory):
    return constraint_factory.forEach(VehicleClass) \
               .filter(lambda vehicle: vehicle.get_total_demand() > vehicle.capacity) \
               .penalize("vehicleCapacity", HardSoftScore.ONE_HARD,
                         lambda vehicle: vehicle.get_total_demand() - vehicle.capacity)


def total_distance(constraint_factory):
    return constraint_factory.forEach(VehicleClass) \
        .penalize("distanceFromPreviousStandstill", HardSoftScore.ONE_SOFT,
                  lambda vehicle: vehicle.get_total_distance_meters())

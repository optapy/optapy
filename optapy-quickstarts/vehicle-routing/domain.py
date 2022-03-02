import math
from random import Random
from optapy import problem_fact, planning_entity, planning_list_variable, planning_solution, planning_score, \
    planning_entity_collection_property, problem_fact_collection_property, value_range_provider
from optapy.score import HardSoftScore

@problem_fact
class Location:
    def __init__(self, _id, latitude, longitude, distance_map=None):
        self.id = _id
        self.latitude = latitude
        self.longitude = longitude
        self.distance_map = distance_map

    def set_distance_map(self, distance_map):
        self.distance_map = distance_map

    def get_distance_to(self, location):
        return self.distance_map[location]

    def get_angle(self, location):
        latitude_difference = location.latitude - self.latitude
        longitude_difference = location.longitude - self.longitude
        return math.atan2(latitude_difference, longitude_difference)

    def to_dict(self):
        return [
            self.latitude,
            self.longitude
        ]

    def __str__(self):
        return f'[{self.latitude}, {self.longitude}]'


@problem_fact
class Customer:
    def __init__(self, _id, location, demand):
        self.id = _id
        self.location = location
        self.demand = demand

    def to_dict(self):
        return {
            'id': self.id,
            'location': self.location.to_dict(),
            'demand': self.demand
        }

    def __str__(self):
        return f'Customer {self.id}'

    def __repr__(self):
        return str(self)


@problem_fact
class Depot:
    def __init__(self, _id, location):
        self.id = _id
        self.location = location

    def to_dict(self):
        return {
            'id': self.id,
            'location': self.location.to_dict(),
        }

    def __str__(self):
        return f'Depot {self.id}'


@planning_entity
class Vehicle:
    def __init__(self, _id, capacity, depot, customer_list=None):
        self.id = _id
        self.capacity = capacity
        self.depot = depot
        if customer_list is None:
            self.customer_list = []
        else:
            self.customer_list = customer_list

    @planning_list_variable(Customer, ['customer_range'])
    def get_customer_list(self):
        return self.customer_list

    def set_customer_list(self, customer_list):
        self.customer_list = customer_list

    def get_route(self):
        if len(self.customer_list) == 0:
            return []
        route = [self.depot.location]
        for customer in self.customer_list:
            route.append(customer.location)
        route.append(self.depot.location)
        return route

    def get_total_demand(self):
        total_demand = 0
        for customer in self.customer_list:
            total_demand += customer.demand
        return total_demand

    def get_total_distance_meters(self):
        total_distance = 0
        last_location = self.depot.location
        for customer in self.customer_list:
            total_distance += customer.location.get_distance_to(last_location)
            last_location = customer.location
        if last_location is not self.depot.location:
            total_distance += self.depot.location.get_distance_to(last_location)
        return total_distance

    def to_dict(self):
        return {
            'id': self.id,
            'capacity': self.capacity,
            'depot': self.depot.to_dict(),
            'customerList': list(map(lambda customer: customer.to_dict(), self.customer_list)),
            'route': list(map(lambda location: location.to_dict(), self.get_route())),
            'totalDemand': self.get_total_demand(),
            'totalDistanceMeters': self.get_total_distance_meters(),
        }

    def __str__(self):
        return f'Vehicle {self.id}'


class EuclideanDistanceCalculator:
    METERS_PER_DEGREE = 111_000

    def calculate_distance(self, start, end):
        if start == end:
            return 0
        latitude_diff = end.latitude - start.latitude
        longitude_diff = end.longitude - start.longitude
        return math.ceil(math.sqrt(latitude_diff * latitude_diff + longitude_diff * longitude_diff) *
                         EuclideanDistanceCalculator.METERS_PER_DEGREE)

    def init_distance_maps(self, location_list):
        for location in location_list:
            distance_map = dict()
            for other_location in location_list:
                distance_map[other_location] = self.calculate_distance(location, other_location)
            location.set_distance_map(distance_map)


class DemoDataBuilder:
    def __init__(self):
        self.southWestCorner = None
        self.northEastCorner = None
        self.customerCount = None
        self.vehicleCount = None
        self.depotCount = None
        self.minDemand = None
        self.maxDemand = None
        self.vehicleCapacity = None
        self.distance_calculator = EuclideanDistanceCalculator()

    @staticmethod
    def builder():
        return DemoDataBuilder()

    def set_south_west_corner(self, southWestCorner):
        self.southWestCorner = southWestCorner
        return self

    def set_north_east_corner(self, northEastCorner):
        self.northEastCorner = northEastCorner
        return self

    def set_min_demand(self, minDemand):
        self.minDemand = minDemand
        return self

    def set_max_demand(self, maxDemand):
        self.maxDemand = maxDemand
        return self

    def set_customer_count(self, customerCount):
        self.customerCount = customerCount
        return self

    def set_vehicle_count(self, vehicleCount):
        self.vehicleCount = vehicleCount
        return self

    def set_depot_count(self, depotCount):
        self.depotCount = depotCount
        return self

    def set_vehicle_capacity(self, vehicleCapacity):
        self.vehicleCapacity = vehicleCapacity
        return self

    def build(self):
        if self.minDemand < 1:
            raise ValueError("minDemand (" + self.minDemand + ") must be greater than zero.")
        if self.maxDemand < 1:
            raise ValueError("maxDemand (" + self.maxDemand + ") must be greater than zero.")
        if self.minDemand >= self.maxDemand:
            raise ValueError("maxDemand (" + self.maxDemand + ") must be greater than minDemand ("
                             + self.minDemand + ").")
        if self.vehicleCapacity < 1:
            raise ValueError("Number of vehicleCapacity (" + self.vehicleCapacity + ") must be greater than zero.")
        if self.customerCount < 1:
            raise ValueError("Number of customerCount (" + self.customerCount + ") must be greater than zero.")
        if self.vehicleCount < 1:
            raise ValueError("Number of vehicleCount (" + self.vehicleCount + ") must be greater than zero.")
        if self.depotCount < 1:
            raise ValueError("Number of depotCount (" + self.depotCount + ") must be greater than zero.")

        if self.northEastCorner.latitude <= self.southWestCorner.latitude:
            raise ValueError("northEastCorner.getLatitude (" + self.northEastCorner.latitude
               + ") must be greater than southWestCorner.getLatitude(" +  self.southWestCorner.latitude + ").")

        if self.northEastCorner.longitude <= self.southWestCorner.longitude:
            raise ValueError("northEastCorner.getLongitude (" + self.northEastCorner.longitude
               + ") must be greater than southWestCorner.getLongitude(" +  self.southWestCorner.longitude + ").")

        name = "demo"

        random = Random(0)
        id_sequence = [0]

        def generate_id():
            out = id_sequence[0]
            id_sequence[0] = out + 1
            return out

        generate_latitude = lambda: random.uniform(self.southWestCorner.latitude, self.northEastCorner.latitude)
        generate_longitude = lambda: random.uniform(self.southWestCorner.longitude, self.northEastCorner.longitude)

        generate_demand = lambda: random.randint(self.minDemand, self.maxDemand)

        depot_list = []
        random_depot = lambda: random.choice(depot_list)

        generate_depot = lambda: Depot(
            generate_id(),
            Location(generate_id(), generate_latitude(), generate_longitude()))

        for i in range(self.depotCount):
            depot_list.append(generate_depot())

        generate_vehicle = lambda: Vehicle(
            generate_id(),
            self.vehicleCapacity,
            random_depot())

        vehicle_list = []
        for i in range(self.vehicleCount):
            vehicle_list.append(generate_vehicle())

        generate_customer = lambda: Customer(
            generate_id(),
            Location(generate_id(), generate_latitude(), generate_longitude()),
            generate_demand())

        customer_list = []
        for i in range(self.customerCount):
            customer_list.append(generate_customer())

        location_list = []
        for customer in customer_list:
            location_list.append(customer.location)
        for depot in depot_list:
            location_list.append(depot.location)

        self.distance_calculator.init_distance_maps(location_list)

        return VehicleRoutingSolution(name, location_list,
                                      depot_list, vehicle_list, customer_list, self.southWestCorner,
                                      self.northEastCorner)


@planning_solution
class VehicleRoutingSolution:
    def __init__(self,  name, location_list, depot_list, vehicle_list, customer_list,
                 south_west_corner, north_east_corner, score=None):
        self.name = name
        self.location_list = location_list
        self.depot_list = depot_list
        self.vehicle_list = vehicle_list
        self.customer_list = customer_list
        self.south_west_corner = south_west_corner
        self.north_east_corner = north_east_corner
        self.score = score

    @staticmethod
    def empty():
        problem = DemoDataBuilder.builder().set_min_demand(1).set_max_demand(2).set_vehicle_capacity(25) \
                                 .set_customer_count(77).set_vehicle_count(6).set_depot_count(2) \
                                 .set_south_west_corner(Location(0, 43.751466, 11.177210)) \
                                 .set_north_east_corner(Location(0, 43.809291, 11.290195)).build()
        problem.set_score(HardSoftScore.ZERO)
        return problem

    @problem_fact_collection_property(Location)
    def get_location_list(self):
        return self.location_list

    @problem_fact_collection_property(Depot)
    def get_depot_list(self):
        return self.depot_list

    @planning_entity_collection_property(Vehicle)
    @value_range_provider('vehicle_range')
    def get_vehicle_list(self):
        return self.vehicle_list

    @problem_fact_collection_property(Customer)
    @value_range_provider('customer_range')
    def get_customer_list(self):
        return self.customer_list

    @planning_score(HardSoftScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score

    def get_bounds(self):
        return [self.south_west_corner, self.north_east_corner]

    def get_distance_meters(self):
        return -self.score.getSoftScore() if self.score is not None else 0

    def to_dict(self):
        return {
            'name': self.name,
            'bounds': list(map(lambda location: location.to_dict(), self.get_bounds())),
            'vehicleList': list(map(lambda vehicle: vehicle.to_dict(), self.vehicle_list)),
            'depotList': list(map(lambda depot: depot.to_dict(), self.depot_list)),
            'customerList': list(map(lambda customer: customer.to_dict(), self.customer_list)),
            'score': str(self.score),
            'distanceMeters': self.get_distance_meters(),
        }

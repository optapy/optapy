from domain import Vehicle, VehicleRoutingSolution
from optapy import solver_manager_create, score_manager_create, get_class
import optapy.config
from optapy.types import Duration
from optapy.score import HardSoftScore
from constraints import vehicle_routing_constraints
from flask import Flask, jsonify
from org.optaplanner.core.api.solver import SolverStatus

app = Flask(__name__)

SINGLETON_ID = 1
solver_config = optapy.config.solver.SolverConfig()
solver_config \
    .withSolutionClass(get_class(VehicleRoutingSolution)) \
    .withEntityClasses(get_class(Vehicle)) \
    .withConstraintProviderClass(get_class(vehicle_routing_constraints)) \
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solver_manager = solver_manager_create(solver_config)
score_manager = score_manager_create(solver_manager)
last_score = HardSoftScore.ZERO

vehicle_routing_solution = VehicleRoutingSolution.empty()


class Status:
    def __init__(self, solution, score_explanation, is_solving):
        self.solution = solution
        self.score_explanation = score_explanation
        self.is_solving = is_solving

    def to_dict(self):
        return {
            'solution': self.solution.to_dict(),
            'scoreExplanation': self.score_explanation,
            'isSolving': self.is_solving
        }


@app.route('/vrp/status', methods=['GET'])
def get_solver_status():
    global vehicle_routing_solution
    return jsonify(Status(vehicle_routing_solution, score_manager.explainScore(vehicle_routing_solution).getSummary(),
                   solver_manager.getSolverStatus(SINGLETON_ID) != SolverStatus.NOT_SOLVING).to_dict())


def error_handler(problem_id, exception):
    print(f'an exception occurred solving {problem_id}: {exception.getMessage()}')
    exception.printStackTrace()


@app.route('/vrp/solve', methods=['POST'])
def solve():
    solver_manager.solveAndListen(SINGLETON_ID, find_by_id, save, error_handler)
    return dict()


@app.route('/vrp/stopSolving', methods=['POST'])
def stop_solving():
    solver_manager.terminateEarly(SINGLETON_ID)
    return dict()


def find_by_id(schedule_id):
    global vehicle_routing_solution
    if schedule_id != SINGLETON_ID:
        raise ValueError(f'There is no schedule with id ({schedule_id})')
    return vehicle_routing_solution


def save(solution):
    global vehicle_routing_solution
    vehicle_routing_solution = solution

from domain import Employee, Shift, Availability, AvailabilityType, ScheduleState, EmployeeSchedule
import datetime
from random import Random
from optapy import solver_manager_create, score_manager_create, get_class
import optapy.config
from optapy.types import Duration
from optapy.score import HardSoftScore
from constraints import employee_scheduling_constraints
from flask import Flask, jsonify

app = Flask(__name__)


def next_weekday(d, weekday):
    days_ahead = weekday - d.weekday()
    if days_ahead <= 0:  # Target day already happened this week
        days_ahead += 7
    return d + datetime.timedelta(days_ahead)


REQUIRED_SKILLS = ["Doctor", "Nurse"]
OPTIONAL_SKILLS = ["Anaesthetics", "Cardiology"]


def reset_application_data():
    ScheduleState.delete_all()
    Employee.delete_all()
    Availability.delete_all()
    Shift.delete_all()


def generate_demo_data():
    INITIAL_ROSTER_LENGTH_IN_DAYS = 14
    START_DATE = next_weekday(datetime.date.today(), 0)  # next Monday

    reset_application_data()

    schedule_state = ScheduleState()
    schedule_state.first_draft_date = START_DATE
    schedule_state.draft_length = INITIAL_ROSTER_LENGTH_IN_DAYS
    schedule_state.publish_length = 7
    schedule_state.last_historic_date = START_DATE
    schedule_state.save()

    FIRST_NAMES = ["Amy", "Beth", "Chad", "Dan", "Elsa", "Flo", "Gus", "Hugo", "Ivy", "Jay"]
    LAST_NAMES = ["Cole", "Fox", "Green", "Jones", "King", "Li", "Poe", "Rye", "Smith", "Watt"]

    random = Random(0)
    name_permutations = join_all_combinations(FIRST_NAMES, LAST_NAMES)
    random.shuffle(name_permutations)

    employee_list = []
    for i in range(15):
        skills = pick_subset(OPTIONAL_SKILLS, random, 3, 1)
        skills.append(pick_random(REQUIRED_SKILLS, random))
        employee = Employee()
        employee.name = name_permutations[i]
        employee.skill_set = skills
        employee.save()
        employee_list.append(employee)

    for i in range(INITIAL_ROSTER_LENGTH_IN_DAYS):
        employees_with_availabilities_on_day = pick_subset(employee_list, random, 4, 3, 2, 1)
        date = START_DATE + datetime.timedelta(days=i)
        for employee in employees_with_availabilities_on_day:
            availability_type = pick_random(AvailabilityType.list(), random)
            availability = Availability()
            availability.date = date
            availability.employee = employee
            availability.availability_type = availability_type
            availability.save()
        generate_shifts_for_day(date, random)


def generate_shifts_for_day(date: datetime.date, random: Random):
    morning_start_time = datetime.datetime.combine(date, datetime.time(hour=6))
    morning_end_time = datetime.datetime.combine(date, datetime.time(hour=14))

    day_start_time = datetime.datetime.combine(date, datetime.time(hour=9))
    day_end_time = datetime.datetime.combine(date, datetime.time(hour=17))

    afternoon_start_time = datetime.datetime.combine(date, datetime.time(hour=14))
    afternoon_end_time = datetime.datetime.combine(date, datetime.time(hour=22))

    night_start_time = datetime.datetime.combine(date, datetime.time(hour=22))
    night_end_time = datetime.datetime.combine(date + datetime.timedelta(days=1), datetime.time(hour=6))

    generate_shift_for_timeslot(morning_start_time, morning_end_time, random)
    generate_shift_for_timeslot(day_start_time, day_end_time, random)
    generate_shift_for_timeslot(afternoon_start_time, afternoon_end_time, random)
    generate_shift_for_timeslot(night_start_time, night_end_time, random)


def generate_shift_for_timeslot(timeslot_start: datetime.datetime, timeslot_end: datetime.datetime, random: Random):
    LOCATIONS = ["Ambulatory care", "Critical care", "Pediatric care"]
    shift_count = random.choices([1, 2], [0.8, 0.2])[0]

    for i in range(shift_count):
        required_skill = None

        if random.randint(0, 1) == 1:
            required_skill = pick_random(REQUIRED_SKILLS, random)
        else:
            required_skill = pick_random(OPTIONAL_SKILLS, random)

        location = pick_random(LOCATIONS, random)
        shift = Shift()
        shift.start = timeslot_start
        shift.end = timeslot_end
        shift.required_skill = required_skill
        shift.location = location
        shift.employee = None
        shift.save()


def pick_random(source: list, random: Random):
    return random.choice(source)


def pick_subset(source: list, random: Random, *distribution: int):
    item_count = random.choices(range(len(distribution)), distribution)
    return random.sample(source, item_count[0])


def join_all_combinations(*part_arrays: list[str]):
    if len(part_arrays) == 0:
        return []
    if len(part_arrays) == 1:
        return part_arrays[0]
    combinations = []
    for combination in join_all_combinations(*part_arrays[1:]):
        for item in part_arrays[0]:
            combinations.append(f'{item} {combination}')
    return combinations


SINGLETON_ID = 1
solver_config = optapy.config.solver.SolverConfig()
solver_config\
    .withSolutionClass(get_class(EmployeeSchedule))\
    .withEntityClasses(get_class(Shift))\
    .withConstraintProviderClass(get_class(employee_scheduling_constraints))\
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solver_manager = solver_manager_create(solver_config)
score_manager = score_manager_create(solver_manager)
last_score = HardSoftScore.ZERO


@app.route('/schedule')
def get_schedule():
    solver_status = get_solver_status()
    solution = EmployeeSchedule(
        ScheduleState.find_all()[0],
        list(Availability.find_all()),
        list(Employee.find_all()),
        list(Shift.find_all()),
        None
    )
    score = score_manager.updateScore(solution)
    solution.solver_status = solver_status
    solution.score = score
    return jsonify(solution.to_dict())


def get_solver_status():
    return solver_manager.getSolverStatus(SINGLETON_ID)


def error_handler(problem_id, exception):
    print(f'an exception occurred solving {problem_id}: {exception.getMessage()}')
    exception.printStackTrace()


@app.route('/solve', methods=['POST'])
def solve():
    solver_manager.solveAndListen(SINGLETON_ID, find_by_id, save, error_handler)
    return dict()


@app.route('/stopSolving', methods=['POST'])
def stop_solving():
    solver_manager.terminateEarly(SINGLETON_ID)
    return dict()


def find_by_id(schedule_id):
    if schedule_id != SINGLETON_ID:
        raise ValueError(f'There is no schedule with id ({schedule_id})')
    schedule = EmployeeSchedule(
        ScheduleState.find_all()[0],
        Availability.find_all(),
        Employee.find_all(),
        Shift.find_all(),
        None
    )
    return schedule


def save(solution):
    for shift in solution.shift_list:
        shift.update()

from domain import Employee, Shift, Availability, AvailabilityType, ScheduleState, EmployeeSchedule
import datetime
from random import Random
from optapy import solver_manager_create, score_manager_create, get_class
import optapy.config
from optapy.types import Duration, SolverStatus
from optapy.score import HardSoftScore
from constraints import employee_scheduling_constraints
from typing import Optional
from flask import Flask, jsonify

app = Flask(__name__)


def next_weekday(d, weekday):
    days_ahead = weekday - d.weekday()
    if days_ahead <= 0:  # Target day already happened this week
        days_ahead += 7
    return d + datetime.timedelta(days_ahead)


FIRST_NAMES = ["Amy", "Beth", "Chad", "Dan", "Elsa", "Flo", "Gus", "Hugo", "Ivy", "Jay"]
LAST_NAMES = ["Cole", "Fox", "Green", "Jones", "King", "Li", "Poe", "Rye", "Smith", "Watt"]
REQUIRED_SKILLS = ["Doctor", "Nurse"]
OPTIONAL_SKILLS = ["Anaesthetics"]
LOCATIONS = ["Ambulatory care", "Critical care", "Pediatric care"]
SHIFT_LENGTH = datetime.timedelta(hours=8)
MORNING_SHIFT_START_TIME = datetime.time(hour=6)
DAY_SHIFT_START_TIME = datetime.time(hour=9)
AFTERNOON_SHIFT_START_TIME = datetime.time(hour=14)
NIGHT_SHIFT_START_TIME = datetime.time(hour=22)

SHIFT_START_TIME_COMBOS = (
    (MORNING_SHIFT_START_TIME, AFTERNOON_SHIFT_START_TIME),
    (MORNING_SHIFT_START_TIME, AFTERNOON_SHIFT_START_TIME, NIGHT_SHIFT_START_TIME),
    (MORNING_SHIFT_START_TIME, DAY_SHIFT_START_TIME, AFTERNOON_SHIFT_START_TIME, NIGHT_SHIFT_START_TIME)
)

location_to_shift_start_time_list_dict = dict()
id_generator = 0
schedule: Optional[EmployeeSchedule] = None


def generate_demo_data():
    global schedule
    INITIAL_ROSTER_LENGTH_IN_DAYS = 14
    START_DATE = next_weekday(datetime.date.today(), 0)  # next Monday

    schedule_state = ScheduleState()
    schedule_state.first_draft_date = START_DATE
    schedule_state.draft_length = INITIAL_ROSTER_LENGTH_IN_DAYS
    schedule_state.publish_length = 7
    schedule_state.last_historic_date = START_DATE

    random = Random(0)

    shift_template_index = 0
    for location in LOCATIONS:
        location_to_shift_start_time_list_dict[location] = SHIFT_START_TIME_COMBOS[shift_template_index]
        shift_template_index = (shift_template_index + 1) % len(SHIFT_START_TIME_COMBOS)

    name_permutations = join_all_combinations(FIRST_NAMES, LAST_NAMES)
    random.shuffle(name_permutations)

    employee_list = []
    for i in range(16):
        skills = pick_subset(OPTIONAL_SKILLS, random, 1, 3)
        skills.append(pick_random(REQUIRED_SKILLS, random))
        employee = Employee()
        employee.name = name_permutations[i]
        employee.skill_set = skills
        employee_list.append(employee)

    shift_list = []
    availability_list = []
    for i in range(INITIAL_ROSTER_LENGTH_IN_DAYS):
        employees_with_availabilities_on_day = pick_subset(employee_list, random, 4, 3, 2, 1)
        date = START_DATE + datetime.timedelta(days=i)
        for employee in employees_with_availabilities_on_day:
            availability_type = pick_random(AvailabilityType.list(), random)
            availability = Availability()
            availability.date = date
            availability.employee = employee
            availability.availability_type = availability_type
            availability_list.append(availability)
        shift_list.extend(generate_shifts_for_day(date, random))
    schedule = EmployeeSchedule(
        schedule_state,
        availability_list,
        employee_list,
        shift_list,
        None
    )


def generate_shifts_for_day(date: datetime.date, random: Random):
    out = []
    for location in LOCATIONS:
        shift_start_time_list = location_to_shift_start_time_list_dict[location]
        for shift_start_time in shift_start_time_list:
            shift_start_date_time = datetime.datetime.combine(date, shift_start_time)
            shift_end_date_time = shift_start_date_time + SHIFT_LENGTH
            out.append(generate_shift_for_timeslot(shift_start_date_time, shift_end_date_time, location, random))
    return out


def generate_shift_for_timeslot(timeslot_start: datetime.datetime, timeslot_end: datetime.datetime,
                                location: str, random: Random):
    global id_generator
    shift_count = random.choices([1, 2], [0.8, 0.2])[0]

    for i in range(shift_count):
        required_skill = None

        if random.randint(0, 1) == 1:
            required_skill = pick_random(REQUIRED_SKILLS, random)
        else:
            required_skill = pick_random(OPTIONAL_SKILLS, random)

        shift = Shift()
        shift.id = id_generator
        shift.start = timeslot_start
        shift.end = timeslot_end
        shift.required_skill = required_skill
        shift.location = location
        shift.employee = None
        id_generator += 1
        return shift


def generate_draft_shifts():
    global schedule
    random = Random(0)
    for i in range(schedule.schedule_state.publish_length):
        employees_with_availabilities_on_day = pick_subset(schedule.employee_list, random, 4, 3, 2, 1)
        date = schedule.schedule_state.first_draft_date + datetime.timedelta(days=(schedule.schedule_state.publish_length + i))
        for employee in employees_with_availabilities_on_day:
            availability_type = pick_random(AvailabilityType.list(), random)
            availability = Availability()
            availability.date = date
            availability.employee = employee
            availability.availability_type = availability_type
            schedule.availability_list.append(availability)
        schedule.shift_list.extend(generate_shifts_for_day(date, random))


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
    .withTerminationSpentLimit(Duration.ofSeconds(60))

solver_manager = solver_manager_create(solver_config)
score_manager = score_manager_create(solver_manager)
last_score = HardSoftScore.ZERO


@app.route('/schedule')
def get_schedule():
    global schedule
    solver_status = get_solver_status()
    solution = schedule
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


@app.route('/publish', methods=['POST'])
def publish():
    global schedule
    if get_solver_status() != SolverStatus.NOT_SOLVING:
        raise RuntimeError('Cannot publish a schedule while solving in progress.')
    schedule_state = schedule.schedule_state
    new_historic_date = schedule_state.first_draft_date
    new_draft_date = schedule_state.first_draft_date + datetime.timedelta(days=schedule_state.publish_length)

    schedule_state.last_historic_date = new_historic_date
    schedule_state.first_draft_date = new_draft_date

    generate_draft_shifts()
    return dict()


@app.route('/stopSolving', methods=['POST'])
def stop_solving():
    solver_manager.terminateEarly(SINGLETON_ID)
    return dict()


def find_by_id(schedule_id):
    global schedule
    if schedule_id != SINGLETON_ID:
        raise ValueError(f'There is no schedule with id ({schedule_id})')
    return schedule


def save(solution):
    global schedule
    schedule = solution

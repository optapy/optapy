import optapy
import optapy.score
import datetime
import enum


@optapy.problem_fact
class Employee:
    def __init__(self, name: str = None, skill_set: list = None):
        self.name = name
        self.skill_set = skill_set

    def __str__(self):
        return f'Employee(name={self.name})'

    def to_dict(self):
        return {
            'name': self.name,
            'skill_set': self.skill_set
        }


class AvailabilityType(enum.Enum):
    DESIRED = 'DESIRED'
    UNDESIRED = 'UNDESIRED'
    UNAVAILABLE = 'UNAVAILABLE'

    @staticmethod
    def list():
        return list(map(lambda at: at, AvailabilityType))


@optapy.problem_fact
class Availability:
    def __init__(self, employee: Employee = None, date: datetime.date = None,
                 availability_type: AvailabilityType = None):
        self.employee = employee
        self.date = date
        self.availability_type = availability_type

    def __str__(self):
        return f'Availability(employee={self.employee}, date={self.date}, availability_type={self.availability_type})'

    def to_dict(self):
        return {
            'employee': self.employee.to_dict(),
            'date': self.date.isoformat(),
            'availability_type': self.availability_type.value
        }


class ScheduleState:
    def __init__(self, publish_length: int = None, draft_length: int = None, first_draft_date: datetime.date = None,
                 last_historic_date: datetime.date = None):
        self.publish_length = publish_length
        self.draft_length = draft_length
        self.first_draft_date = first_draft_date
        self.last_historic_date = last_historic_date

    def is_draft(self, shift):
        return shift.start >= datetime.datetime.combine(self.first_draft_date, datetime.time.min)

    def to_dict(self):
        return {
            'publish_length': self.publish_length,
            'draft_length': self.draft_length,
            'first_draft_date': self.first_draft_date.isoformat(),
            'last_historic_date': self.last_historic_date.isoformat()
        }


def shift_pinning_filter(solution, shift):
    return not solution.schedule_state.is_draft(shift)


@optapy.planning_entity(pinning_filter=shift_pinning_filter)
class Shift:
    def __init__(self, start: datetime.datetime = None, end: datetime.datetime = None,
                 location: str = None, required_skill: str = None, employee: Employee = None):
        self.start = start
        self.end = end
        self.location = location
        self.required_skill = required_skill
        self.employee = employee

    @optapy.planning_id
    def get_id(self):
        return self.id

    @optapy.planning_variable(Employee, value_range_provider_refs=['employee_range'])
    def get_employee(self):
        return self.employee

    def set_employee(self, employee):
        self.employee = employee

    def __str__(self):
        return f'Shift(start={self.start}, end={self.end}, location={self.location}, ' \
               f'required_skill={self.required_skill}, employee={self.employee})'

    def to_dict(self):
        return {
            'start': self.start.isoformat(),
            'end': self.end.isoformat(),
            'location': self.location,
            'required_skill': self.required_skill,
            'employee': self.employee.to_dict() if self.employee is not None else None
        }


@optapy.planning_solution
class EmployeeSchedule:
    def __init__(self, schedule_state, availability_list, employee_list, shift_list, solver_status, score=None):
        self.employee_list = employee_list
        self.availability_list = availability_list
        self.schedule_state = schedule_state
        self.shift_list = shift_list
        self.solver_status = solver_status
        self.score = score

    @optapy.problem_fact_collection_property(Employee)
    @optapy.value_range_provider('employee_range')
    def get_employee_list(self):
        return self.employee_list

    @optapy.problem_fact_collection_property(Availability)
    def get_availability_list(self):
        return self.availability_list

    @optapy.planning_entity_collection_property(Shift)
    def get_shift_list(self):
        return self.shift_list

    @optapy.planning_score(optapy.score.HardSoftScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score

    def to_dict(self):
        return {
            'employee_list': list(map(lambda employee: employee.to_dict(), self.employee_list)),
            'availability_list': list(map(lambda availability: availability.to_dict(), self.availability_list)),
            'schedule_state': self.schedule_state.to_dict(),
            'shift_list': list(map(lambda shift: shift.to_dict(), self.shift_list)),
            'solver_status': self.solver_status.toString(),
            'score': self.score.toString(),
        }

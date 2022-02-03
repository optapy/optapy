from optapy import constraint_provider, get_class
from optapy.score import HardSoftScore
from optapy.constraint import Joiners, ConstraintFactory

from domain import Shift, Availability, AvailabilityType
from datetime import timedelta, datetime

shift_class = get_class(Shift)
availability_class = get_class(Availability)


def get_start_of_availability(availability: Availability):
    return datetime.combine(availability.date, datetime.min.time())


def get_end_of_availability(availability: Availability):
    return datetime.combine(availability.date, datetime.max.time())


def get_minute_overlap(shift1: Shift, shift2: Shift):
    duration_of_overlap: timedelta = min(shift1.end, shift2.end) - max(shift1.start, shift2.start)
    return duration_of_overlap.total_seconds() // 60


def get_shift_duration_in_minutes(shift: Shift):
    return (shift.end - shift.start).total_seconds() // 60


@constraint_provider
def employee_scheduling_constraints(constraint_factory: ConstraintFactory):
    return [
        required_skill(constraint_factory),
        no_overlapping_shifts(constraint_factory),
        at_least_10_hours_between_two_shifts(constraint_factory),
        one_shift_per_day(constraint_factory),
        unavailable_employee(constraint_factory),
        desired_day_for_employee(constraint_factory),
        undesired_day_for_employee(constraint_factory),
    ]


def required_skill(constraint_factory: ConstraintFactory):
    return constraint_factory.forEach(shift_class) \
        .filter(lambda shift: shift.required_skill not in shift.employee.skill_set) \
        .penalize("Missing required skill", HardSoftScore.ONE_HARD)


def no_overlapping_shifts(constraint_factory: ConstraintFactory):
    return constraint_factory.forEachUniquePair(shift_class, [Joiners.equal(lambda shift: shift.employee),
                                                Joiners.overlapping(lambda shift: shift.start,
                                                                    lambda shift: shift.end)]) \
                             .penalize("Overlapping shift", HardSoftScore.ONE_HARD, get_minute_overlap)


def at_least_10_hours_between_two_shifts(constraint_factory: ConstraintFactory):
    TEN_HOURS_IN_SECONDS = 60 * 60 * 10
    return constraint_factory.forEachUniquePair(shift_class, [
                                                Joiners.equal(lambda shift: shift.employee),
                                                Joiners.lessThanOrEqual(lambda shift: shift.end,
                                                                        lambda shift: shift.start)]) \
                             .filter(lambda first_shift, second_shift:
                                     (second_shift.start - first_shift.end).total_seconds() < TEN_HOURS_IN_SECONDS) \
                             .penalize("At least 10 hours between 2 shifts", HardSoftScore.ONE_HARD,
                                       lambda first_shift, second_shift:
                                       TEN_HOURS_IN_SECONDS - ((second_shift.start - first_shift.end).total_seconds() //
                                                               60))


def one_shift_per_day(constraint_factory: ConstraintFactory):
    return constraint_factory.forEachUniquePair(shift_class, [Joiners.equal(lambda shift: shift.employee),
                                                Joiners.equal(lambda shift: shift.start.date())]) \
                             .penalize("Max one shift per day", HardSoftScore.ONE_HARD)


def unavailable_employee(constraint_factory: ConstraintFactory):
    return constraint_factory.forEach(shift_class) \
        .join(availability_class, [Joiners.equal(lambda shift: shift.employee,
                                                 lambda availability: availability.employee),
                                   Joiners.equal(lambda shift: shift.start.date(),
                                                 lambda availability: availability.date)
                                   ]) \
        .filter(lambda shift, availability: availability.availability_type == AvailabilityType.UNAVAILABLE) \
        .penalize('Unavailable employee', HardSoftScore.ONE_HARD,
                  lambda shift, availability: get_shift_duration_in_minutes(shift))


def desired_day_for_employee(constraint_factory: ConstraintFactory):
    return constraint_factory.forEach(shift_class) \
        .join(availability_class, [Joiners.equal(lambda shift: shift.employee,
                                                 lambda availability: availability.employee),
                                   Joiners.equal(lambda shift: shift.start.date(),
                                                 lambda availability: availability.date)
                                   ]) \
        .filter(lambda shift, availability: availability.availability_type == AvailabilityType.DESIRED) \
        .penalize('Desired day for employee', HardSoftScore.ONE_SOFT,
                  lambda shift, availability: get_shift_duration_in_minutes(shift))


def undesired_day_for_employee(constraint_factory: ConstraintFactory):
    return constraint_factory.forEach(shift_class) \
        .join(availability_class, [Joiners.equal(lambda shift: shift.employee,
                                                 lambda availability: availability.employee),
                                   Joiners.equal(lambda shift: shift.start.date(),
                                                 lambda availability: availability.date)
                                   ]) \
        .filter(lambda shift, availability: availability.availability_type == AvailabilityType.UNDESIRED) \
        .penalize('Undesired day for employee', HardSoftScore.ONE_SOFT,
                  lambda shift, availability: get_shift_duration_in_minutes(shift))

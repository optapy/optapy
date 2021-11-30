from optapy import constraint_provider, get_class
from optapy.score import HardSoftScore
from optapy.constraint import ConstraintFactory, Joiners
from domain import Lesson, Room
from datetime import datetime, date, timedelta

LessonClass = get_class(Lesson)
RoomClass = get_class(Room)

# Trick since timedelta only works with datetime instances
today = date.today()


def within_30_minutes(lesson1, lesson2):
    between = datetime.combine(today, lesson1.timeslot.end_time) - datetime.combine(today, lesson2.timeslot.start_time)
    return timedelta(minutes=0) <= between <= timedelta(minutes=30)


# Type annotation not needed, but allows you to get autocompletion
@constraint_provider
def define_constraints(constraint_factory: ConstraintFactory):
    return [
        # Hard constraints
        room_conflict(constraint_factory),
        teacher_conflict(constraint_factory),
        student_group_conflict(constraint_factory),
        # Soft constraints
        teacher_room_stability(constraint_factory),
        teacher_time_efficiency(constraint_factory),
        student_group_subject_variety(constraint_factory)
    ]


def room_conflict(constraint_factory: ConstraintFactory):
    # A room can accommodate at most one lesson at the same time.
    return constraint_factory \
        .forEach(LessonClass) \
        .join(LessonClass,
              [
                  # ... in the same timeslot ...
                  Joiners.equal(lambda lesson: lesson.timeslot),
                  # ... in the same room ...
                  Joiners.equal(lambda lesson: lesson.room),
                  # form unique pairs
                  Joiners.lessThan(lambda lesson: lesson.id)
              ]) \
        .penalize("Room conflict", HardSoftScore.ONE_HARD)


def teacher_conflict(constraint_factory: ConstraintFactory):
    # A teacher can teach at most one lesson at the same time.
    return constraint_factory \
        .forEach(LessonClass) \
        .join(LessonClass,
              [
                  Joiners.equal(lambda lesson: lesson.timeslot),
                  Joiners.equal(lambda lesson: lesson.teacher),
                  Joiners.lessThan(lambda lesson: lesson.id)
              ]) \
        .penalize("Teacher conflict", HardSoftScore.ONE_HARD)


def student_group_conflict(constraint_factory: ConstraintFactory):
    # A student can attend at most one lesson at the same time.
    return constraint_factory \
        .forEach(LessonClass) \
        .join(LessonClass,
              [
                  Joiners.equal(lambda lesson: lesson.timeslot),
                  Joiners.equal(lambda lesson: lesson.student_group),
                  Joiners.lessThan(lambda lesson: lesson.id)
              ]) \
        .penalize("Student group conflict", HardSoftScore.ONE_HARD)


def teacher_room_stability(constraint_factory: ConstraintFactory):
    # A teacher prefers to teach in a single room.
    return constraint_factory \
        .forEach(LessonClass) \
        .join(LessonClass,
              [
                  Joiners.equal(lambda lesson: lesson.teacher),
                  Joiners.lessThan(lambda lesson: lesson.id)
              ]) \
        .filter(lambda lesson1, lesson2: lesson1.room != lesson2.room) \
        .penalize("Teacher room stability", HardSoftScore.ONE_SOFT)


def teacher_time_efficiency(constraint_factory: ConstraintFactory):
    # A teacher prefers to teach sequential lessons and dislikes gaps between lessons.
    return constraint_factory.forEach(LessonClass) \
        .join(LessonClass,
              [
                  Joiners.equal(lambda lesson: lesson.teacher),
                  Joiners.equal(lambda lesson: lesson.timeslot.day_of_week)
              ]) \
        .filter(within_30_minutes) \
        .reward("Teacher time efficiency", HardSoftScore.ONE_SOFT)


def student_group_subject_variety(constraint_factory: ConstraintFactory):
    # A student group dislikes sequential lessons on the same subject.
    return constraint_factory.forEach(LessonClass) \
        .join(LessonClass,
              [
                  Joiners.equal(lambda lesson: lesson.subject),
                  Joiners.equal(lambda lesson: lesson.student_group),
                  Joiners.equal(lambda lesson: lesson.timeslot.day_of_week)
              ]) \
        .filter(within_30_minutes) \
        .penalize("Student group subject variety", HardSoftScore.ONE_SOFT)
